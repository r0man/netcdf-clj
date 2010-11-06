(ns netcdf.test.model
  (:require [netcdf.dods :as dods])
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
        clojure.test
        clojure.contrib.mock
        netcdf.model
        netcdf.test.helper))

(deftest test-find-model-by-name
  (is (= (find-model-by-name "akw")
         {:name "akw"
          :url "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
          :description "Regional Alaska Waters Wave Model"}))
  (is (nil? (find-model-by-name "unknown"))))

(deftest test-reference-times  
  (with-test-inventory
    (let [reference-times (reference-times (find-model-by-name "nww3"))]
      (is (= 2 (count reference-times))))))

(deftest test-latest-reference-time
  (with-test-inventory
    (is (= (latest-reference-time (find-model-by-name "nww3"))
           (date-time 2010 10 30 6)))))

(deftest test-local-path
  (is (= (local-path (find-model-by-name "akw") "htsgwsfc" (date-time 2010 11 5 6))
         "./akw/htsgwsfc/20101105T060000Z.nc"))
  (is (= (local-path (find-model-by-name "akw") "htsgwsfc" (date-time 2010 11 5 6) "/tmp")
         "/tmp/akw/htsgwsfc/20101105T060000Z.nc")))

(deftest test-local-uri
  (is (= (local-uri (find-model-by-name "akw") "htsgwsfc" (date-time 2010 11 5 6))
         (java.net.URI. "file:./akw/htsgwsfc/20101105T060000Z.nc")))
  (is (= (local-uri (find-model-by-name "akw") "htsgwsfc" (date-time 2010 11 5 6) "/tmp")
         (java.net.URI. "file:/tmp/akw/htsgwsfc/20101105T060000Z.nc"))))

(deftest test-find-dataset
  (with-test-inventory
    (let [dataset (find-dataset (find-model-by-name "nww3"))]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z")))
    (let [dataset (find-dataset (find-model-by-name "nww3") (date-time 2010 10 30 0))]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_00z")))))

(deftest test-copy-variable
  (with-test-inventory
    (let [filename "/tmp/test-copy-variable"
          dataset-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z"]
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))]
        (is (= (copy-variable (find-model-by-name "nww3") "htsgwsfc" filename) filename)))
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))]
        (is (= (copy-variable (find-model-by-name "nww3") "htsgwsfc" filename (date-time 2010 10 30 6)) filename))))))
