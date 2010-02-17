(ns netcdf.test.projection
  (:use clojure.test netcdf.location netcdf.point netcdf.projection))

(deftest test-location->point
  (let [point (location->point (make-location 0 0))]
    (is (= (:x point) 0))
    (is (= (:y point) 0))))

(deftest test-point->location
  (let [location (point->location (make-point 0 0))]
    (is (= (:latitude location) 0))
    (is (= (:longitude location ) 0))))

