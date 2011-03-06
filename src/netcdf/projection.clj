(ns netcdf.projection
  (:import ucar.unidata.geoloc.Projection
           (ucar.unidata.geoloc LatLonPoint LatLonPointImpl ProjectionPoint ProjectionPointImpl LatLonRect))
  (:use netcdf.location))

(defn location->xy [^Projection projection location]
  (let [point (. projection latLonToProj location (ProjectionPointImpl.))]
    [(. point getX) (. point getY)]))

(defn location->row-column [^Projection projection location]
  (reverse (location->xy projection location)))

(defn forward-mapping
  "Transforms the geographic coordinates (latitude, longitude) on the
  curved reference surface to a set of planar Cartesian
  coordinates (x, y), representing the position of the same point on
  the map plane."
  [^Projection projection ^LatLonPoint location & [^ProjectionPoint point]]
  (. projection latLonToProj location (or (ProjectionPointImpl.) point)))

(defn backward-mapping
  "Transforms the planar Cartesian coordinates (x, y) of a point on
  the map plane to a set of geographic coordinates (latitude,
  longitude) on the curved reference surface."
  [^Projection projection ^ProjectionPoint point & [^LatLonPoint location]]
  (. projection projToLatLon point (or location (LatLonPointImpl.))))
