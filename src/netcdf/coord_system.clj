(ns netcdf.coord-system
  (:import ucar.nc2.dt.GridCoordSystem)
  (:use netcdf.location
        netcdf.test.helper))

(defn latitude-axis
  "Returns the latitude axis of the coordinate system."
  [^GridCoordSystem coord-system]
  (let [axis (.getYHorizAxis coord-system)
        bounds (.getLatLonBoundingBox coord-system)]
    {:min (.getLatMin bounds)
     :max (.getLatMax bounds)
     :size (int (.getSize axis))
     :step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn longitude-axis
  "Returns the longitude axis of the coordinate system."
  [^GridCoordSystem coord-system]
  (let [axis (.getXHorizAxis coord-system)
        bounds (.getLatLonBoundingBox coord-system)]
    {:min (.getLonMin bounds)
     :max (.getLonMax bounds)
     :size (int (.getSize axis))
     :step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn location-axis
  "Returns the latitude and longitude axis of the coordinate system."
  [^GridCoordSystem coord-system]
  {:latitude-axis (latitude-axis coord-system)
   :longitude-axis (longitude-axis coord-system)})

(defn projection
  "Returns the projection of the coordinate system"
  [^GridCoordSystem coord-system] (.getProjection coord-system))

(defn x-y-index
  "Find the x and y indexes of the location."
  [^GridCoordSystem coord-system location]
  (vec (. coord-system findXYindexFromLatLon (latitude location) (longitude location) nil)))

(defn location-on-grid
  "Returns the nearset location on the grid."
  [^GridCoordSystem coord-system location]
  (let [[x y] (x-y-index coord-system location)]
    (.getLatLon coord-system x y)))

