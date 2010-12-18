(ns netcdf.geo-grid
  (:import incanter.Matrix
           org.joda.time.DateTime
           ucar.nc2.dt.grid.GeoGrid
           ucar.nc2.dt.grid.GridDataset
           ucar.nc2.dt.GridCoordSystem)
  (:use [incanter.core :only (matrix ncol nrow sel)]
        [clj-time.coerce :only (from-date to-date)]
        [clj-time.format :only (unparse parse formatters)]
        [netcdf.coord-system :only (x-y-index)]
        [clojure.contrib.duck-streams :only (write-lines)]
        [clojure.string :only (join)]))

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

(defn lat-axis
  "Returns the latitude axis of the GeoGrid."
  [^GeoGrid grid]
  (let [axis (. (coord-system grid) getYHorizAxis)
        bounds (bounding-box grid)]
    {:min (.getLatMin bounds)
     :max (.getLatMax bounds)
     :size (int (.getSize axis))
     :step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn lon-axis
  "Returns the longitude axis of the GeoGrid."
  [^GeoGrid grid]
  (let [axis (. (coord-system grid) getXHorizAxis)
        bounds (bounding-box grid)
        projection (.getProjection (coord-system grid))]
    {:min (.getLonMin bounds)
     :max (.getLonMax bounds)
     :size (int (.getSize axis))
     :step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn lat-lon-axis
  "Returns the latitude and longitude axis of the GeoGrid."
  [^GeoGrid grid]
  {:lat-axis (lat-axis grid)
   :lon-axis (lon-axis grid)})

(defn meta-data
  "Returns the meta data of the GeoGrid."
  [^GeoGrid grid]
  {:name (.getName grid)
   :description (.getDescription grid)
   :lat-axis (lat-axis grid)
   :lon-axis (lon-axis grid)})

(defn open-geo-grid
  "Open a NetCDF GeoGrid."
  [dataset-uri variable] (. (. GridDataset open (str dataset-uri)) findGridDatatype variable))

(defn projection
  "Returns the projection of the GeoGrid."
  [^GeoGrid grid] (.getProjection (coord-system grid)))

(defn time-axis
  "Returns the time axis of the GeoGrid."
  [^GeoGrid grid] (.getTimeAxis1D (coord-system grid)))

(defn vertical-axis
  "Returns the vertical axis of the GeoGrid."
  [^GeoGrid grid] (.getVerticalAxis (coord-system grid)))

(defn valid-times
  "Returns the valid times of the NetCDF GeoGrid."
  [^GeoGrid grid] (map from-date (.getTimeDates (time-axis grid))))

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
      (for [^int x-index (range 0 (.getLength (.getXDimension grid)))
            ^int y-index (range 0 (.getLength (.getYDimension grid)))]
        {:location (.getLatLon coord-system x-index y-index)
         :variable (.getName grid)
         :valid-time valid-time
         :value (.getDouble (. grid readDataSlice t-index z-index y-index x-index) 0)})
      (assoc (meta-data grid) :valid-time valid-time))))

(defn read-matrix
  "Read the whole GeoGrid as a matrix."
  [^GeoGrid grid & {:keys [valid-time z-coord]}]
  (let [sequence (read-seq grid :valid-time valid-time :z-coord z-coord)]
    (with-meta (matrix (map :value sequence) (:size (:lon-axis (meta sequence))))
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

(defn write-csv [^GeoGrid grid filename & {:keys [remove valid-time z-coord separator]}]
  (let [format-fn #(format-record % :separator separator)
        records (read-seq grid :valid-time valid-time :z-coord z-coord)]
    (write-lines filename (map format-record (if remove (clojure.core/remove remove records) records)))))

;; (write-csv
;;  (open-geo-grid "/home/roman/.netcdf/nww3/htsgwsfc/2010/12/18/120000Z.nc" "htsgwsfc")
;;  "/tmp/nww3.csv"
;;  :remove #(Double/isNaN (:value %)))

;; (write-csv
;;  (open-geo-grid "/home/roman/.netcdf/nww3/htsgwsfc/2010/12/18/060000Z.nc" "htsgwsfc")
;;  "/tmp/nww3.csv")

;; (defn write-csv-seq [records filename]
;;   (let [separator (or separator ",")]
;;     (with-out-writer filename
;;       (for [record (read-seq grid :valid-time valid-time :z-coord z-coord)]
;;         (println
;;          (join
;;           ))))))

;; (def *nww3* (open-geo-grid "/home/roman/.netcdf/nww3/htsgwsfc/20100828/t12z.nc" "htsgwsfc"))

;; (meta (read-seq *nww3*))

;; (def bb (bounding-box *nww3*))
;; (.getBoundingBox (coord-system *nww3*))

;; (time
;;  (write-geo-grid-as-csv *nww3* "/tmp/test.csv"))

;; (def *nww3* (open-geo-grid "/tmp/netcdf-test.nc" "htsgwsfc"))

;; (meta-data *nww3*)
;; (bounding-box *nww3*)
;; (.getDefaultMapAreaLL (projection *nww3*))

;; (read-location *nww3* {:latitude 76 :longitude 0})

;; (defn location->row-column [^Matrix matrix location]
;;   (let [meta (meta matrix) [row column] (projection/location->row-column (:projection meta) location )]
;;     [(int (/ (+ (* row -1) (int (/ (nrow matrix) 2))) (:step (:lat-axis meta))))
;;      (int (/ column (:step (:lon-axis meta))))]))

;; (defn sel-location
;;   "Select the data point in the matrix for the location."
;;   [^Matrix matrix location & options]
;;   (let [options (apply hash-map options)
;;         [row column] (location->row-column matrix location)]
;;     (if (and (>= column 0) (< column (ncol matrix))
;;              (>= row 0) (< row (nrow matrix)))
;;       (sel matrix row column))))

;; (defn sel-location!
;;   "Select the data point in the matrix for the location."
;;   [^Matrix matrix location]
;;   (apply sel matrix (location->row-column matrix location)))

;; (def grid (open-geo-grid "/home/roman/.netcdf/nww3/htsgwsfc/20100607/t00z.nc" "htsgwsfc"))
;; (def grid (open-geo-grid "http://nomad5.ncep.noaa.gov:9090/dods/gfs2p5/gfs20100828/gfs2p5_00z" "tmpsfc"))

;; (.getDescription grid)

;; (defn save-matrix-meta [^GeoGrid grid filename & {:keys [valid-time z-coord]}]
;;   (spit filename
;;         {:lat-axis (lat-axis grid)
;;          :lon-axis (lon-axis grid)
;;          :name (.getName grid)
;;          :valid-time valid-time}))

;; (defn save-matrix [^GeoGrid grid filename & {:keys [valid-time z-coord]}]
;;   (let [matrix (read-matrix grid :valid-time valid-time :z-coord z-coord)
;;         meta (meta matrix)]
;;     (spit filename
;;           (assoc meta
;; :valid-time (unparse (formatters :basic-date-time-no-ms) (:valid-time meta))
;;             :data matrix))))

;; (defn load-matrix [filename]
;;   (read-string (slurp filename)))

;; (clj-time.format/show-formatters)

;; (save-matrix grid "/tmp/matrix")
;; (load-matrix "/tmp/matrix")

;; (meta (read-matrix grid))
