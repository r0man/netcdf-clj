(ns netcdf.geo-grid
  (:import java.util.Date ucar.nc2.dt.grid.GeoGrid incanter.Matrix)
  (:use incanter.core)
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

(defn latitude-axis
  "Returns the latitude axis of the geo grid."
  [#^GeoGrid geo-grid]
  (let [axis (. (coord-system geo-grid) getYHorizAxis) bounds (bounding-box geo-grid)]
    {:lat-min (.getLatMin bounds)
     :lat-max (.getLatMax bounds)
     :lat-size (int (.getSize axis))
     :lat-step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn longitude-axis
  "Returns the longitude axis of the geo grid."
  [#^GeoGrid geo-grid]
  (let [axis (. (coord-system geo-grid) getXHorizAxis) bounds (bounding-box geo-grid)]
    {:lon-min (.getLonMin bounds)
     :lon-max (.getLonMax bounds)
     :lon-size (int (.getSize axis))
     :lon-step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn lat-lon-axis
  "Returns the latitude and longitude axis of the geo grid."
  [#^GeoGrid geo-grid] (merge (latitude-axis geo-grid) (longitude-axis geo-grid)))

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
