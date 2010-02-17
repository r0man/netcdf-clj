(ns netcdf.projection
  (:import clojure.lang.PersistentStructMap
           ucar.unidata.geoloc.projection.Mercator
           ucar.unidata.geoloc.LatLonPointImpl
           ucar.unidata.geoloc.ProjectionPointImpl)
  (:use netcdf.location netcdf.position))

(def *projection* (Mercator. 0 0))

(defmacro with-projection [projection & body]
  `(binding [*projection* ~projection]
     ~@body))

(defmulti location->position class)

(defmethod location->position LatLonPointImpl [location]
  (let [position (. *projection* latLonToProj location (ProjectionPointImpl.))]
    (make-position (. position getX) (. position getY))))

(defmethod location->position PersistentStructMap [location]
  (location->position (LatLonPointImpl. (:latitude location) (:longitude location))))

(defmulti position->location class)

(defmethod position->location ProjectionPointImpl [position]
  (let [location (. *projection* projToLatLon position (LatLonPointImpl.))]
    (make-location (. location getLatitude) (. location getLongitude))))

(defmethod position->location PersistentStructMap [position]
  (position->location (ProjectionPointImpl. (:x position) (:y position))))
