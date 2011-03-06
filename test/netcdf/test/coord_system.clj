(ns netcdf.test.coord-system
  (:import ucar.unidata.geoloc.Projection)
  (:require [netcdf.geo-grid :as grid])
  (:use clojure.test
        netcdf.location
        netcdf.coord-system
        netcdf.test.helper))

(def *coord-system* (grid/coord-system (grid/open-geo-grid *dataset-uri* *variable*)))

(deftest test-projection
  (is (isa? (class (projection *coord-system*)) Projection)))

(deftest test-x-y-index
  (is (= (x-y-index *coord-system* (make-location 0 0)) [0 78])))

(deftest test-location-on-grid
  (are [location expected]
    (is (= expected (location-on-grid *coord-system* location)))
    (make-location 0 0) (make-location 0 0)
    (make-location 78 0) (make-location 78 0)
    (make-location 77.5 0) (make-location 78 0)
    (make-location 77.4 0) (make-location 77 0)
    (make-location 78 0.624) (make-location 78 0)
    (make-location 78 0.625) (make-location 78 1.25)))
