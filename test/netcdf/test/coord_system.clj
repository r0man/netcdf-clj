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

(deftest test-latitude-axis
  (let [axis (latitude-axis *coord-system*)]
    (is (= (:min axis) -78))
    (is (= (:max axis) 78))
    (is (= (:size axis) 157))
    (is (= (:step axis) 1))))

(deftest test-longitude-axis
  (let [axis (longitude-axis *coord-system*)]
    (is (= (:min axis) 0))
    (is (= (:max axis) 358.75))
    (is (= (:size axis) 288))
    (is (= (:step axis) 1.25))))

