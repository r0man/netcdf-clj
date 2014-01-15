(ns netcdf.projection-point-test
  (:use clojure.test netcdf.projection-point))

(deftest test-make-projection-point
  (let [point (make-projection-point 1 2)]
    (is (projection-point? point))
    (is (= (.getX point) 1.0))
    (is (= (.getY point) 2.0))))

(deftest test-projection-point?
  (is (not (projection-point? nil)))
  (is (not (projection-point? "")))
  (is (projection-point? (make-projection-point 1 2))))
