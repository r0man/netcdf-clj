(ns netcdf.test.model
  (:import java.io.File java.net.URI)
  (:require [netcdf.dods :as dods])
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
        clojure.test
        clojure.contrib.mock
        netcdf.model
        netcdf.variable
        netcdf.test.helper))

(deftest test-reference-times
  (with-test-inventory
    (let [reference-times (reference-times nww3)]
      (is (= 2 (count reference-times))))))

(deftest test-latest-reference-time
  (with-test-inventory
    (is (= (latest-reference-time nww3)
           (date-time 2010 10 30 6)))))

(deftest test-local-path
  (is (= (local-path akw htsgwsfc (date-time 2010 11 5 6))
         (str *root-dir* "/akw/htsgwsfc/2010/11/5/060000Z.nc")))
  (is (= (local-path akw htsgwsfc (date-time 2010 11 5 6) "/tmp")
         "/tmp/akw/htsgwsfc/2010/11/5/060000Z.nc")))

(deftest test-local-uri
  (is (= (local-uri akw htsgwsfc (date-time 2010 11 5 6))
         (URI. (str "file:" *root-dir* "/akw/htsgwsfc/2010/11/5/060000Z.nc"))))
  (is (= (local-uri akw htsgwsfc (date-time 2010 11 5 6) "/tmp")
         (URI. "file:/tmp/akw/htsgwsfc/2010/11/5/060000Z.nc"))))

(deftest test-find-dataset
  (with-test-inventory
    (let [dataset (find-dataset nww3)]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z")))
    (let [dataset (find-dataset nww3 (date-time 2010 10 30 0))]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_00z")))))

(deftest test-download-variable
  (with-test-inventory
    (let [filename (local-path nww3 htsgwsfc (date-time 2010 10 30 6))
          dataset-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z"]
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))]
        (is (download-variable nww3 htsgwsfc)))
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))]
        (is (download-variable nww3 htsgwsfc :reference-time (date-time 2010 10 30 6)))))))

(deftest test-download-gfs
  (with-test-inventory
    (expect [copy-dataset (has-args [] (returns ""))]
      (is (download-gfs)))))

(deftest test-download-wave-watch
  (with-test-inventory
    (expect [copy-dataset (has-args [] (returns ""))]
      (is (download-wave-watch)))))
