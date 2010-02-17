(ns netcdf.test.projection
  (:use clojure.test netcdf.location netcdf.position netcdf.projection))

(deftest test-location->position
  (let [position (location->position (make-location 0 0))]
    (is (= (:x position) 0))
    (is (= (:y position) 0))))

(deftest test-position->location
  (let [location (position->location (make-position 0 0))]
    (is (= (:latitude location) 0))
    (is (= (:longitude location ) 0))))

