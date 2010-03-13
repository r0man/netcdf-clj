(ns netcdf.test.projection
  (:import ucar.unidata.geoloc.projection.LatLonProjection)
  (:use clojure.test netcdf.location netcdf.projection))

(def *projection* (LatLonProjection.))

(deftest test-location->xy
  (are [latitude longitude x y]
    (is (= (location->xy *projection* (make-location latitude longitude)) [x y]))
    0 0 0 0))

(deftest test-location->row-column
  (are [latitude longitude row column]
    (is (= (location->row-column *projection* (make-location latitude longitude)) [row column]))
    0 0 0 0))
