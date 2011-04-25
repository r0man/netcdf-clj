(ns netcdf.test.resolution
  (:use clojure.test
        netcdf.resolution))

(def akw (make-resolution 0.5 0.25))
(def nww3 (make-resolution 1.25 1.0))

(deftest test-make-resolution
  (let [resolution (make-resolution 1.25 1.0)]
    (is (= 1.25 (:width resolution)))
    (is (= 1.0 (:height resolution)))))

(deftest test-sort-resolutions
  (is (= [akw nww3] (sort-resolutions [akw nww3])))
  (is (= [akw nww3] (sort-resolutions [nww3 akw]))))

(deftest test-min-resolution
  (is (= nww3 (min-resolution [akw nww3]))))

(deftest test-max-resolution
  (is (= akw (max-resolution [akw nww3]))))
