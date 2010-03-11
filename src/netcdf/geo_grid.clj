(ns netcdf.geo-grid
  (:import java.util.Date ucar.nc2.dt.grid.GeoGrid incanter.Matrix)
  (:use incanter.core netcdf.location)
  (:require [netcdf.dataset :as dataset]))

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
  (let [axis (. (coord-system geo-grid) getYHorizAxis) bounds (bounding-box geo-grid)]
    {:lat-min (.getLatMin bounds)
     :lat-max (.getLatMax bounds)
     :lat-size (int (.getSize axis))
     :lat-step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn lon-axis
  "Returns the longitude axis of the geo grid."
  [#^GeoGrid geo-grid]
  (let [axis (. (coord-system geo-grid) getXHorizAxis) bounds (bounding-box geo-grid)]
    {:lon-min (.getLonMin bounds)
     :lon-max (.getLonMax bounds)
     :lon-size (int (.getSize axis))
     :lon-step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn lat-lon-axis
  "Returns the latitude and longitude axis of the geo grid."
  [#^GeoGrid geo-grid] (merge (lat-axis geo-grid) (lon-axis geo-grid)))

(defn open-geo-grid
  "Open the NetCDF geo grid."
  [dataset-uri variable] (. (dataset/open-grid-dataset dataset-uri) findGridDatatype variable))

(defn projection
  "Returns the projection of the geo grid."
  [#^GeoGrid geo-grid] (.getProjection (coord-system geo-grid)))

(defn valid-times
  "Returns the valid times in the NetCDF geo grid."
  [#^GeoGrid geo-grid] (.. (coord-system geo-grid) getTimeAxis1D getTimeDates))

(defn time-index
  "Returns the time index into the geo grid for valid-time."
  [#^GeoGrid geo-grid #^Date valid-time]
  (. (. (coord-system geo-grid) getTimeAxis1D) findTimeIndexFromDate valid-time))

(defn- read-xy-data [#^GeoGrid geo-grid #^Date valid-time & [z-index]]  
  (. geo-grid readYXData (time-index geo-grid valid-time) (or z-index 0)))

(defn read-seq
  "Read the whole geo grid as a sequence."
  [#^GeoGrid geo-grid & options]
  (let [options (apply hash-map options) valid-time (or (:valid-time options) (first (valid-times geo-grid)))]
    (with-meta (seq (. (read-xy-data geo-grid valid-time (:z-index options)) copyTo1DJavaArray))
      (merge (axis geo-grid) {:valid-time valid-time}))))

(defn read-matrix
  "Read the whole geo grid as a matrix."
  [#^GeoGrid geo-grid & options]
  (let [sequence (apply read-seq geo-grid options)]
    (with-meta (.viewRowFlip (matrix sequence (:lon-size (meta sequence))))
      (meta sequence))))

(defn latitude->row [#^Matrix matrix #^Double latitude]
  (let [{:keys [lat-max lat-step]} (meta matrix)]
    (* -1 (- (/ latitude lat-step) lat-max))))

(defn longitude->column [#^Matrix matrix #^Double longitude]
  (let [{:keys [lon-max lon-step]} (meta matrix)]
    (/ longitude lon-step)))

(defn location->row-col
  [#^Matrix matrix location]
  [(latitude->row matrix (latitude location))
   (longitude->column matrix (longitude location))])

(defn sel-location
  "Select the data point in the matrix for the location."
  [#^Matrix matrix location]
  (let [[row column] (location->row-col matrix location)]
    (if (and (> column 0) (< column (ncol matrix))
             (> row 0) (< row (nrow matrix)))
      (sel matrix row column))))

(defn sel-location!
  "Select the data point in the matrix for the location."
  [#^Matrix matrix location]
  (let [[row column] (location->row-col matrix location)]
    (sel matrix row column)))

(time
 (dotimes [i (* 256 256)]
   (sel-location *matrix* (make-location 0 0))))

;; (sel-location *matrix* (make-location 80 300))
;; (location->row-col *matrix* (make-location 79 0))

;; (location->row-col *matrix* (make-location 78 1.25))
;; (location->row-col *matrix* (make-location 78 90))

;; (longitude->column *matrix* 1.25)

;; (sel-location *matrix* (make-location -79 10))

;; (println (meta *matrix*))

;; (def *matrix* (read-matrix (open-geo-grid "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))

;; (count *matrix*)

;; (println
;;  (sel *matrix*
;;       (range 0 5)
;;       (range 0 5)))


