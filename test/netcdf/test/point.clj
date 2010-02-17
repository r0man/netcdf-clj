(ns netcdf.test.point
  (:use clojure.test netcdf.point))

(deftest test-make-point
  (let [point (make-point 1 2)]
    (is (= (:x point) 1))
    (is (= (:y point) 2))))

(deftest test-point?
  (is (point? (make-point 1 2)))
  (is (not (point? nil)))
  (is (not (point? {:x nil :y 2})))
  (is (not (point? {:x nil :y 2}))))

