(ns netcdf.bounding-box
  (:import (ucar.unidata.geoloc LatLonPoint LatLonRect))
  (:use netcdf.location))

(defn make-bounding-box
  ([location-1 location-2]
     (let [location-1 (parse-location location-1)
           location-2 (parse-location location-2)]
       (LatLonRect. location-1  location-2)))
  ([latitude-1 longitude-1 latitude-2 longitude-2]
     (make-bounding-box
      (make-location latitude-1 longitude-1)
      (make-location latitude-2 longitude-2))))

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
