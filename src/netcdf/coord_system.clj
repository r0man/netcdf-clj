(ns netcdf.coord-system
  (:import ucar.nc2.dt.GridCoordSystem)
  (:use netcdf.location
        netcdf.resolution))

(defn make-axis
  "Returns an axis."
  [min max step]
  {:min min
   :max max
   :size (int (+ (* (- max min) step) step))
   :step step})

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
    (try (.getLatLon coord-system x y)
         (catch ArrayIndexOutOfBoundsException _ nil))))

(defn fraction-of-latitudes [^GridCoordSystem coord-system location-1 location-2]
  (let [step (:step (latitude-axis coord-system))]
    (/ (- (latitude location-1) (latitude location-2))
       step)))

(defn fraction-of-longitudes [^GridCoordSystem coord-system location-1 location-2]
  (let [step (:step (longitude-axis coord-system))]
    (/ (- (longitude location-2) (longitude location-1))
       step)))

(defn max-axis
  "Returns the greatest of the axis."
  ([x] x)
  ([x y]
     (make-axis
      (min (:min x) (:min y))
      (max (:max x) (:max y))
      (min (:step x) (:step y))))
  ([x y & more]
     (reduce max-axis (max-axis x y) more)))

(defn min-axis
  "Returns the least of the axis."
  ([x] x)
  ([x y]
     (make-axis
      (max (:min x) (:min y))
      (min (:max x) (:max y))
      (max (:step x) (:step y))))
  ([x y & more]
     (reduce min-axis (min-axis x y) more)))

(defn resolution
  "Returns the resolution of the coordinate system."
  [^GridCoordSystem coord-system]
  (make-resolution
   (/ (.getWidth (.getLatLonBoundingBox coord-system))
      (- (.getSize (.getXHorizAxis coord-system)) 1))
   (/ (.getHeight (.getLatLonBoundingBox coord-system))
      (- (.getSize (.getYHorizAxis coord-system)) 1))))
