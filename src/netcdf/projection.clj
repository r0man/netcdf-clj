(ns netcdf.projection
  (:import clojure.lang.PersistentStructMap
           ucar.unidata.geoloc.projection.Mercator
           ucar.unidata.geoloc.LatLonPointImpl
           ucar.unidata.geoloc.ProjectionPointImpl)
  (:use netcdf.location netcdf.point))

(def *projection* (Mercator. 0 0))

(defmacro with-projection [projection & body]
  `(binding [*projection* ~projection]
     ~@body))

(defmulti location->point
  "Convert location to projection coordinates."
  class)

(defmethod location->point LatLonPointImpl [location]
  (let [point (. *projection* latLonToProj location (ProjectionPointImpl.))]
    (make-point (. point getX) (. point getY))))

(defmethod location->point PersistentStructMap [location]
  (location->point (LatLonPointImpl. (:latitude location) (:longitude location))))

(defmulti point->location
  "Convert projection coordinates to a location."
  class)

(defmethod point->location ProjectionPointImpl [point]
  (let [location (. *projection* projToLatLon point (LatLonPointImpl.))]
    (make-location (. location getLatitude) (. location getLongitude))))

(defmethod point->location PersistentStructMap [point]
  (point->location (ProjectionPointImpl. (:x point) (:y point))))
