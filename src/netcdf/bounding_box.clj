(ns netcdf.bounding-box
  (:import (ucar.unidata.geoloc LatLonPointImpl LatLonRect))
  (:use netcdf.location))

(defmulti make-bounding-box
  (fn [& args]
    (let [num (count args)]
     (cond
      (= num 1) String
      (= num 2) [(class (first args)) (class (last args))]))))

(defmethod make-bounding-box String [bounds-spec]
  (LatLonRect. bounds-spec))

(defmethod make-bounding-box [String String] [location-1 location-2]
  (LatLonRect. (parse-location location-1) (parse-location location-2)))


