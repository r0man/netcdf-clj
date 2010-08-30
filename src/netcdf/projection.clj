(ns netcdf.projection
  (:import ucar.unidata.geoloc.Projection
           (ucar.unidata.geoloc LatLonPointImpl ProjectionPointImpl LatLonRect))
  (:use netcdf.location netcdf.point))

(defn location->xy [#^Projection projection location]
  (let [point (. projection latLonToProj location (ProjectionPointImpl.))]
    [(. point getX) (. point getY)]))

(defn location->row-column [#^Projection projection location]
  (reverse (location->xy projection location)))

;; (def *nww3* (netcdf.geo-grid/open-geo-grid "/tmp/netcdf-test.nc" "htsgwsfc"))
;; (def *projection* (.. *nww3* getCoordinateSystem getProjection))

;; *projection*

;; (.latLonToProjRect
;;  *projection* 
;;  (LatLonRect.
;;   (LatLonPointImpl. -78 -180)
;;   (LatLonPointImpl. 78 180)))
