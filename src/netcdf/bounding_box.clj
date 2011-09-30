(ns netcdf.bounding-box
  (:import (ucar.unidata.geoloc LatLonPoint LatLonRect))
  (:use [clojure.string :only (split)]
        netcdf.location))

(defprotocol IBoundingBox
  (to-bounding-box [object] "Convert object into a bounding box."))

(defn make-bounding-box
  "Make a new LatLonRect."
  ([location-1 location-2]
     (LatLonRect.
      (to-location location-1)
      (to-location location-2)))
  ([latitude-1 longitude-1 latitude-2 longitude-2]
     (make-bounding-box
      (make-location latitude-1 longitude-1)
      (make-location latitude-2 longitude-2))))

(defn parse-bounding-box
  "Parse string and make a new LatLonRect."
  [string] (apply make-bounding-box (split string #",|\s+")))

(defn to-map [bounding-box]
  {:north-west
   {:latitude (.getLatitude (.getUpperLeftPoint bounding-box))
    :longitude (.getLongitude (.getUpperLeftPoint bounding-box))}
   :north-east
   {:latitude (.getLatitude (.getUpperRightPoint bounding-box))
    :longitude (.getLongitude (.getUpperRightPoint bounding-box)) }
   :south-east
   {:latitude (.getLatitude (.getLowerRightPoint bounding-box))
    :longitude (.getLongitude (.getLowerRightPoint bounding-box))}
   :south-west
   {:latitude (.getLatitude (.getLowerLeftPoint bounding-box))
    :longitude (.getLongitude (.getLowerLeftPoint bounding-box))}})

(extend-type LatLonRect
  IBoundingBox
  (to-bounding-box [object]
    object))

(extend-type clojure.lang.IPersistentMap
  IBoundingBox
  (to-bounding-box [map]
    (make-bounding-box (:south-west map) (:north-east map))))
