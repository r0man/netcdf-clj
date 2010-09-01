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
  "Transforms mathematically the planar Cartesian coordinates (x, y)
  of a point on the map plane to a set of geographic
  coordinates (latitude, longitude) on the curved reference surface."
  [^Projection projection ^ProjectionPoint point & [^LatLonPoint location]]
  (. projection projToLatLon point (or location (LatLonPointImpl.))))

;; (def *nww3* (netcdf.geo-grid/open-geo-grid "/tmp/netcdf-test.nc" "htsgwsfc"))
;;; (def *projection* (.. *nww3* getCoordinateSystem getProjection))

;; (.projToLatLon *projection* 0.0 0.0)
;; (.projToLatLon *projection* 180.0 0.0)
;; (.projToLatLon *projection* 157.0 0.0)

;; (seq (float-array 2 [1.1 1.2]))

;; (map seq (.projToLatLon *projection* (into-array [(float-array 2 [0.0 0.0]) (float-array 2 [1.0 1.0])])))

;; *projection*

;; (.latLonToProjRect
;;  *projection* 
;;  (LatLonRect.
;;   (LatLonPointImpl. -78 -180)
;;   (LatLonPointImpl. 78 180)))
