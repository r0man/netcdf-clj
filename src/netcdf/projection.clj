(ns netcdf.projection
  (:import ucar.unidata.geoloc.Projection
           (ucar.unidata.geoloc LatLonPointImpl ProjectionPointImpl))
  (:use netcdf.location netcdf.point))

(defn location->xy [#^Projection projection location]
  (let [point (. projection latLonToProj location (ProjectionPointImpl.))]
    [(. point getX) (. point getY)]))

(defn location->row-column [#^Projection projection location]
  (reverse (location->xy projection location)))

