(ns netcdf.geo-grid
  (:import incanter.Matrix
           org.joda.time.DateTime
           ucar.nc2.dt.grid.GeoGrid)
  (:require [netcdf.dataset :as dataset]
            [netcdf.projection :as projection])
  (:use [incanter.core :only (matrix ncol nrow sel)]
        [clj-time.coerce :only (from-date to-date)]
        [clj-time.format :only (unparse parse formatters)]))

(defn coord-system
  "Returns the coordinate system of the GeoGrid."
  [#^GeoGrid geo-grid] (.getCoordinateSystem geo-grid))

(defn bounding-box
  "Returns the bounding box of the GeoGrid."
  [#^GeoGrid geo-grid] (.getLatLonBoundingBox (coord-system geo-grid)))

(defn description
  "Returns the description of the GeoGrid."
  [#^GeoGrid geo-grid] (.. geo-grid getVariable getDescription))

(defn lat-axis
  "Returns the latitude axis of the GeoGrid."
  [#^GeoGrid geo-grid]
  (let [axis (. (coord-system geo-grid) getYHorizAxis)
        bounds (bounding-box geo-grid)]
    {:min (.getLatMin bounds)
     :max (.getLatMax bounds)
     :size (int (.getSize axis))
     :step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn- normalize-lon-axis [axis]
  (if (<= (:max axis) 180)
    axis
    (let [diff (/ (- (:max axis) (:min axis)) 2)]
      (assoc axis
        :min (- (:min axis) diff)
        :max (- (:max axis) diff)))))

(defn lon-axis
  "Returns the longitude axis of the GeoGrid."
  [#^GeoGrid geo-grid]
  (let [axis (. (coord-system geo-grid) getXHorizAxis)
        bounds (bounding-box geo-grid)]
    (normalize-lon-axis
     {:min (.getLonMin bounds)
      :max (.getLonMax bounds)
      :size (int (.getSize axis))
      :step (/ (.getWidth bounds) (- (.getSize axis) 1))})))

(defn lat-lon-axis
  "Returns the latitude and longitude axis of the GeoGrid."
  [#^GeoGrid geo-grid]
  {:lat-axis (lat-axis geo-grid)
   :lon-axis (lon-axis geo-grid)})

(defn meta-data
  "Returns the meta data of the GeoGrid."
  [#^GeoGrid geo-grid]
  {:name (.getName geo-grid)
   :description (.getDescription geo-grid)
   :lat-axis (lat-axis geo-grid)
   :lon-axis (lon-axis geo-grid)})

(defn open-geo-grid
  "Open a NetCDF GeoGrid."
  [dataset-uri variable] (. (dataset/open-grid-dataset dataset-uri) findGridDatatype variable))

(defn projection
  "Returns the projection of the GeoGrid."
  [#^GeoGrid geo-grid] (.getProjection (coord-system geo-grid)))

(defn time-axis
  "Returns the time axis of the GeoGrid."
  [#^GeoGrid geo-grid] (.getTimeAxis1D (coord-system geo-grid)))

(defn valid-times
  "Returns the valid times of the NetCDF GeoGrid."
  [#^GeoGrid geo-grid] (map from-date (.getTimeDates (time-axis geo-grid))))

(defn time-index
  "Returns the GeoGrid time index for valid-time."
  [#^GeoGrid geo-grid #^DateTime valid-time]
  (. (time-axis geo-grid) findTimeIndexFromDate (to-date valid-time)))

(defn- read-xy-data [#^GeoGrid geo-grid #^DateTime valid-time & [z-index]]  
  (seq (.copyTo1DJavaArray (. geo-grid readYXData (time-index geo-grid valid-time) (or z-index 0)))))

(defn read-seq
  "Read the whole GeoGrid as a sequence."
  [#^GeoGrid geo-grid & {:keys [valid-time z-index]}]
  (let [valid-time (or valid-time (first (valid-times geo-grid)))]
    (with-meta (read-xy-data geo-grid valid-time z-index)
      (assoc (meta-data geo-grid) :valid-time valid-time))))

(defn read-matrix
  "Read the whole GeoGrid as a matrix."
  [#^GeoGrid geo-grid & {:keys [valid-time z-index]}]
  (let [sequence (read-seq geo-grid :valid-time valid-time :z-index z-index)]
    (with-meta (.viewRowFlip (matrix sequence (:size (:lon-axis (meta sequence)))))
      (meta sequence))))

;; (defn location->row-column [#^Matrix matrix location]
;;   (let [meta (meta matrix) [row column] (projection/location->row-column (:projection meta) location )]
;;     [(int (/ (+ (* row -1) (int (/ (nrow matrix) 2))) (:step (:lat-axis meta))))
;;      (int (/ column (:step (:lon-axis meta))))]))

;; (defn sel-location
;;   "Select the data point in the matrix for the location."
;;   [#^Matrix matrix location & options]
;;   (let [options (apply hash-map options)
;;         [row column] (location->row-column matrix location)]
;;     (if (and (>= column 0) (< column (ncol matrix))
;;              (>= row 0) (< row (nrow matrix)))
;;       (sel matrix row column))))

;; (defn sel-location!
;;   "Select the data point in the matrix for the location."
;;   [#^Matrix matrix location]   
;;   (apply sel matrix (location->row-column matrix location)))

;; (def grid (open-geo-grid "/home/roman/.netcdf/nww3/htsgwsfc/20100607/t00z.nc" "htsgwsfc"))
;; (def grid (open-geo-grid "http://nomad5.ncep.noaa.gov:9090/dods/gfs2p5/gfs20100828/gfs2p5_00z" "tmpsfc"))

;; (.getDescription grid)

;; (defn save-matrix-meta [#^GeoGrid geo-grid filename & {:keys [valid-time z-index]}]
;;   (spit filename
;;         {:lat-axis (lat-axis geo-grid)
;;          :lon-axis (lon-axis geo-grid)
;;          :name (.getName geo-grid)
;;          :valid-time valid-time}))

;; (defn save-matrix [#^GeoGrid geo-grid filename & {:keys [valid-time z-index]}]
;;   (let [matrix (read-matrix geo-grid :valid-time valid-time :z-index z-index)
;;         meta (meta matrix)]
;;     (spit filename
;;           (assoc meta
;;             :valid-time (unparse (formatters :basic-date-time-no-ms) (:valid-time meta))
;;             :data matrix))))

;; (defn load-matrix [filename]
;;   (read-string (slurp filename)))

;; (clj-time.format/show-formatters)

;; (save-matrix grid "/tmp/matrix")
;; (load-matrix "/tmp/matrix")

;; (meta (read-matrix grid))
