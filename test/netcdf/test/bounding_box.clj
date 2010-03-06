(ns netcdf.test.bounding-box
  (:use clojure.test netcdf.bounding-box))

(deftest test-make-bounding-box-with-string
  (let [bounds (make-bounding-box "1,2 10,20")]
    (is (= (.getLatMin bounds) 1))
    (is (= (.getLatMax bounds) 11))
    (is (= (.getLonMin bounds) 2))
    (is (= (.getLonMax bounds) 22))))

(deftest test-make-bounding-box-with-strings
  (let [bounds (make-bounding-box "1,2" "10,20")]
    (is (= (.getLatMin bounds) 1))
    (is (= (.getLatMax bounds) 10))
    (is (= (.getLonMin bounds) 2))
    (is (= (.getLonMax bounds) 20))))
