(ns netcdf.geo-grid
  (:import incanter.Matrix
           org.joda.time.DateTime
           ucar.nc2.dt.grid.GeoGrid)
  (:require [netcdf.dataset :as dataset]
            [netcdf.projection :as projection])
  (:use [incanter.core :only (matrix ncol nrow sel)]
        [clj-time.coerce :only (from-date to-date)]))

(defn coord-system
  "Returns the coordinate system of the geo grid."
  [#^GeoGrid geo-grid] (.getCoordinateSystem geo-grid))

(defn bounding-box
  "Returns the bounding box of the geo grid."
  [#^GeoGrid geo-grid] (.getLatLonBoundingBox (coord-system geo-grid)))

(defn description
  "Returns the description of the geo grid."
  [#^GeoGrid geo-grid] (.. geo-grid getVariable getDescription))

(defn lat-axis
  "Returns the latitude axis of the geo grid."
  [#^GeoGrid geo-grid]
  (let [axis (. (coord-system geo-grid) getYHorizAxis)
        bounds (bounding-box geo-grid)]
    {:min (.getLatMin bounds)
     :max (.getLatMax bounds)
     :size (int (.getSize axis))
     :step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn lon-axis
  "Returns the longitude axis of the geo grid."
  [#^GeoGrid geo-grid]
  (let [axis (. (coord-system geo-grid) getXHorizAxis)
        bounds (bounding-box geo-grid)]
    {:min (.getLonMin bounds)
     :max (.getLonMax bounds)
     :size (int (.getSize axis))
     :step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn lat-lon-axis
  "Returns the latitude and longitude axis of the geo grid."
  [#^GeoGrid geo-grid]
  {:lat-axis (lat-axis geo-grid)
   :lon-axis (lon-axis geo-grid)})

(defn open-geo-grid
  "Open the NetCDF geo grid."
  [dataset-uri variable] (. (dataset/open-grid-dataset dataset-uri) findGridDatatype variable))

(defn projection
  "Returns the projection of the geo grid."
  [#^GeoGrid geo-grid] (.getProjection (coord-system geo-grid)))

(defn time-axis
  "Returns the time axis of the geo grid for valid-time."
  [#^GeoGrid geo-grid] (.getTimeAxis1D (coord-system geo-grid)))

(defn valid-times
  "Returns the valid times in the NetCDF geo grid."
  [#^GeoGrid geo-grid] (map from-date (.getTimeDates (time-axis geo-grid))))

(defn time-index
  "Returns the time index into the geo grid for valid-time."
  [#^GeoGrid geo-grid #^DateTime valid-time]
  (. (time-axis geo-grid) findTimeIndexFromDate (to-date valid-time)))

(defn- read-xy-data [#^GeoGrid geo-grid #^DateTime valid-time & [z-index]]  
  (. geo-grid readYXData (time-index geo-grid valid-time) (or z-index 0)))

(defn read-seq
  "Read the whole geo grid as a sequence."
  [#^GeoGrid geo-grid & options]
  (let [options (apply hash-map options) valid-time (or (:valid-time options) (first (valid-times geo-grid)))]
    (with-meta (seq (. (read-xy-data geo-grid valid-time (:z-index options)) copyTo1DJavaArray))
      (merge (lat-lon-axis geo-grid) {:valid-time valid-time :projection (projection geo-grid)}))))

(defn read-matrix
  "Read the whole geo grid as a matrix."
  [#^GeoGrid geo-grid & options]
  (let [sequence (apply read-seq geo-grid options)]
    (with-meta (.viewRowFlip (matrix sequence (:size (:lon-axis (meta sequence)))))
      (meta sequence))))

(defn location->row-column [#^Matrix matrix location]
  (let [meta (meta matrix) [row column] (projection/location->row-column (:projection meta) location )]
    [(int (/ (+ (* row -1) (int (/ (nrow matrix) 2))) (:step (:lat-axis meta))))
     (int (/ column (:step (:lon-axis meta))))]))

(defn sel-location
  "Select the data point in the matrix for the location."
  [#^Matrix matrix location & options]
  (let [options (apply hash-map options)
        [row column] (location->row-column matrix location)]
    (if (and (>= column 0) (< column (ncol matrix))
             (>= row 0) (< row (nrow matrix)))
      (sel matrix row column))))

(defn sel-location!
  "Select the data point in the matrix for the location."
  [#^Matrix matrix location]   
  (apply sel matrix (location->row-column matrix location)))
