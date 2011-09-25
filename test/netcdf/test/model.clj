(ns netcdf.test.model
  (:require [netcdf.dods :as dods])
  (:use [clj-time.coerce :only (to-long)]
        [clj-time.core :only (date-time interval plus minutes minus)]
        clojure.test
        netcdf.bounding-box
        netcdf.location
        netcdf.model
        netcdf.repository
        netcdf.test.helper
        netcdf.time))

(deftest test-find-model-by-location
  (let [models [akw nww3 wna]]
    (are [location model]
      (is (= model (find-model-by-location models location)))
      (.getLowerLeftPoint (:bounding-box akw)) akw
      (.getLowerLeftPoint (:bounding-box wna)) wna
      (.getLowerLeftPoint (:bounding-box nww3)) nww3
      (make-location 43.4073349 -2.6983217) nww3)))

(deftest test-make-model
  (let [model akw]
    (is (= "akw" (:name model)))
    (is (= "Regional Alaska Waters Wave Model" (:description model)))
    (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw" (:dods model)))
    (is (.equals (make-bounding-box 44.75 159.5 75.25 -123.5) (:bounding-box model)))
    (is (= {:latitude 0.25 :longitude 0.5} (:resolution model)))))

(deftest test-model?
  (is (not (model? nil)))
  (is (not (model? "")))
  (is (model? nww3)))

(deftest test-reference-times
  (with-test-inventory
    (let [reference-times (dods/reference-times nww3)]
      (is (< (to-long (first reference-times))
             (to-long (second reference-times))))
      (is (= 2 (count reference-times))))))

(deftest test-sort-by-resolution
  (is (= [akw wna nww3] (sort-by-resolution [wna akw nww3]))))
