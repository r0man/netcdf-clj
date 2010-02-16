(ns netcdf.test.position
  (:use clojure.test netcdf.position))

(deftest test-make-position
  (let [position (make-position 1 2)]
    (is (= (:x position) 1))
    (is (= (:y position) 2))))

(deftest test-position?
  (is (position? (make-position 1 2)))
  (is (not (position? nil)))
  (is (not (position? {:x nil :y 2})))
  (is (not (position? {:x nil :y 2}))))

