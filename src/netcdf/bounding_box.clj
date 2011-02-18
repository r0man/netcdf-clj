(ns netcdf.bounding-box
  (:import (ucar.unidata.geoloc LatLonPoint LatLonRect))
  (:use netcdf.location))

(defmulti make-bounding-box
  (fn [location-1 location-2]
    [(class location-1) (class location-2)]))

(defmethod make-bounding-box [String String] [location-1 location-2]
  (make-bounding-box (parse-location location-1) (parse-location location-2)))

(defmethod make-bounding-box [LatLonPoint LatLonPoint] [location-1 location-2]
  (if (< (longitude location-1) (longitude location-2))
    (LatLonRect. location-1  location-2)
    (LatLonRect. location-2  location-1)))

(defn to-map [bounding-box]
  {:north-west {:latitude (.getLatitude (.getUpperLeftPoint bounding-box))
                :longitude (.getLongitude (.getUpperLeftPoint bounding-box))}
   :north-east {:latitude (.getLatitude (.getUpperRightPoint bounding-box))
                :longitude (.getLongitude (.getUpperRightPoint bounding-box)) }
   :south-east {:latitude (.getLatitude (.getLowerRightPoint bounding-box))
                :longitude (.getLongitude (.getLowerRightPoint bounding-box))}
   :south-west {:latitude (.getLatitude (.getLowerLeftPoint bounding-box))
                :longitude (.getLongitude (.getLowerLeftPoint bounding-box))}})
