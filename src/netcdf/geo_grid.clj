(ns netcdf.geo-grid
  (:import (ucar.nc2.dt.grid GeoGrid GridDataset)
           incanter.Matrix
           org.joda.time.DateTime
           ucar.nc2.dt.GridCoordSystem)
  (:use [clj-time.format :only (formatters parse unparse)]
        [clojure.contrib.duck-streams :only (write-lines)]
        [clojure.string :only (join)]
        [incanter.core :only (matrix ncol nrow sel view)]
        netcdf.coord-system
        netcdf.interpolation
        netcdf.location
        netcdf.time))

(defn coord-system
  "Returns the coordinate system of the GeoGrid."
  [^GeoGrid grid] (.getCoordinateSystem grid))

(defn bounding-box
  "Returns the bounding box of the GeoGrid."
  [^GeoGrid grid] (.getLatLonBoundingBox (coord-system grid)))

(defn description
  "Returns the description of the GeoGrid."
  [^GeoGrid grid] (.. grid getVariable getDescription))

(defn dimensions
  "Returns the dimensions of the GeoGrid."
  [^GeoGrid grid] (seq (.getDimensions grid)))

(defn format-record [record & {:keys [separator]}]
  (join (or separator ",")
        [(str (unparse (formatters :year-month-day) (:valid-time record)) " "
              (unparse (formatters :hour-minute-second) (:valid-time record)))
         (:variable record)
         (.getLatitude (:location record))
         (.getLongitude (:location record))
         (:value record)]))

(defn filter-records [records]
  (remove #(Double/isNaN (:value %)) records))

(defn meta-data
  "Returns the meta data of the GeoGrid."
  [^GeoGrid grid]
  (assoc (location-axis (coord-system grid))
    :name (.getName grid)
    :description (.getDescription grid)))

(defn open-geo-grid
  "Open a NetCDF GeoGrid."
  [dataset-uri variable] (. (. GridDataset open (str dataset-uri)) findGridDatatype variable))

(defn time-axis
  "Returns the time axis of the GeoGrid."
  [^GeoGrid grid] (.getTimeAxis1D (coord-system grid)))

(defn vertical-axis
  "Returns the vertical axis of the GeoGrid."
  [^GeoGrid grid] (.getVerticalAxis (coord-system grid)))

(defn valid-times
  "Returns the valid times of the NetCDF GeoGrid."
  [^GeoGrid grid] (map to-date-time (.getTimeDates (time-axis grid))))

(defn time-index
  "Returns the GeoGrid time index for valid-time."
  [^GeoGrid grid ^DateTime valid-time]
  (. (time-axis grid) findTimeIndexFromDate (to-date valid-time)))

(defn z-index
  "Returns the z-index into the GeoGrid for the z-coordinate."
  [^GeoGrid grid z-coord]
  (if-let [vertical-axis (vertical-axis grid)]
    (. vertical-axis findCoordElement z-coord) 0))

(defn- read-yx-data [^GeoGrid grid ^DateTime valid-time & [z-coord]]
  (seq (.copyTo1DJavaArray (. grid readYXData (time-index grid valid-time) (z-index grid z-coord)))))

(defn read-seq
  "Read the whole GeoGrid as a sequence."
  [^GeoGrid grid & {:keys [valid-time z-coord]}]
  (let [valid-time (or valid-time (first (valid-times grid)))]
    (with-meta (read-yx-data grid valid-time z-coord)
      (assoc (meta-data grid) :valid-time valid-time))))

(defn read-seq
  "Read the whole GeoGrid as a sequence."
  [^GeoGrid grid & {:keys [valid-time z-coord]}]
  (let [valid-time (or valid-time (first (valid-times grid)))
        ^GridCoordSystem  coord-system (coord-system grid)
        ^int t-index (time-index grid valid-time)
        ^int z-index (z-index grid z-coord)]
    (with-meta
      (for [^int y-index (range 0 (.getLength (.getYDimension grid)))
            ^int x-index (range 0 (.getLength (.getXDimension grid)))]
        {:location (.getLatLon coord-system x-index y-index)
         :variable (.getName grid)
         :valid-time valid-time
         :value (.getDouble (. grid readDataSlice t-index z-index y-index x-index) 0)})
      (assoc (meta-data grid) :valid-time valid-time))))

(defn read-matrix
  "Read the whole GeoGrid as a matrix."
  [^GeoGrid grid & {:keys [valid-time z-coord]}]
  (let [sequence (read-seq grid :valid-time valid-time :z-coord z-coord)]
    (with-meta (.viewRowFlip (matrix (map :value sequence) (:size (:longitude-axis (meta sequence)))))
      (meta sequence))))

(defn read-x-y [^GeoGrid grid x y & {:keys [valid-time z-coord]}]
  (if (and x y)
    (let [t-index (time-index grid (or valid-time (first (valid-times grid))))
          z-index (z-index grid z-coord)]
      (.getDouble (. grid readDataSlice t-index z-index y x) 0))))

(defn read-location [^GeoGrid grid location & {:keys [valid-time z-coord]}]
  (if location
    (let [t-index (time-index grid (or valid-time (first (valid-times grid))))
          z-index (z-index grid z-coord)
          [x-index y-index] (x-y-index (coord-system grid) location)]
      (.getDouble (. grid readDataSlice t-index z-index y-index x-index) 0))))

(defn read-locations [^GeoGrid grid locations & options]
  (map #(apply read-location grid % options) locations))

(defn interpolate-location [^GeoGrid grid location & {:keys [valid-time z-coord width height]}]
  (if location
    (if-let [locations (sample-locations (coord-system grid) location :width width :height height)]
      (let [values (read-locations grid locations :valid-time valid-time :z-coord z-coord)]
        (interpolate
         (matrix (map #(if (Double/isNaN %) 0 %) values) 2)
         (fraction-of-longitudes (coord-system grid) (first locations) location)
         (fraction-of-latitudes (coord-system grid) (first locations) location))))))

(defn interpolate-locations [^GeoGrid grid locations & options]
  (map #(apply interpolate-location grid % options) locations))

(defn write-csv [^GeoGrid grid filename & {:keys [remove valid-time z-coord separator]}]
  (let [format-fn #(format-record % :separator separator)
        records (read-seq grid :valid-time valid-time :z-coord z-coord)]
    (write-lines filename (map format-record (if remove (clojure.core/remove remove records) records)))))

(defmacro with-open-geo-grid [[name uri variable] & body]
  `(with-open [dataset# (. GridDataset open (str ~uri))]
     (let [~name (.findGridDatatype dataset# ~variable)]
       ~@body)))
