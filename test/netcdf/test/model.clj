(ns netcdf.test.model
  (:import java.io.File java.net.URI org.joda.time.Interval)
  (:require [netcdf.dods :as dods])
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
        clojure.test
        clojure.contrib.mock
        netcdf.model
        netcdf.variable
        netcdf.test.helper))

(def *akw*
  (make-model
   :description "Regional Alaska Waters Wave Model"
   :name "akw"
   :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
   :variables wave-watch-variables))

(deftest test-make-model
  (let [model *akw*]
    (is (= "akw" (:name model)))
    (is (= "Regional Alaska Waters Wave Model" (:description model)))
    (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw" (:dods model)))
    (is (= wave-watch-variables (:variables model)))))

(deftest test-reference-times
  (with-test-inventory
    (let [reference-times (reference-times nww3)]
      (is (= 2 (count reference-times))))))

(deftest test-latest-reference-time
  (with-test-inventory
    (is (= (latest-reference-time nww3)
           (date-time 2010 10 30 6)))))

(deftest test-variable-path
  (is (= (str *root-dir* "/akw/htsgwsfc/2010/11/05/060000Z.nc")
         (variable-path akw htsgwsfc (date-time 2010 11 5 6))))
  (is (= "/tmp/akw/htsgwsfc/2010/11/05/060000Z.nc"
         (variable-path akw htsgwsfc (date-time 2010 11 5 6) "/tmp")))
  (is (= "/tmp/akw/htsgwsfc/2010/11/05/060000Z.nc"
         (variable-path akw htsgwsfc (date-time 2010 11 5 6) "/tmp")))
  (is (= "s3n://burningswell/netcdf/akw/htsgwsfc/2010/11/05/060000Z.nc"
         (variable-path akw htsgwsfc (date-time 2010 11 5 6) "s3n://burningswell/netcdf"))))

(deftest test-local-uri
  (is (= (local-uri akw htsgwsfc (date-time 2010 11 5 6))
         (URI. (str "file:" *root-dir* "/akw/htsgwsfc/2010/11/05/060000Z.nc"))))
  (is (= (local-uri akw htsgwsfc (date-time 2010 11 5 6) "/tmp")
         (URI. "file:/tmp/akw/htsgwsfc/2010/11/05/060000Z.nc"))))

(deftest test-find-dataset
  (with-test-inventory
    (let [dataset (find-dataset nww3)]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z")))
    (let [dataset (find-dataset nww3 (date-time 2010 10 30 0))]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_00z")))))

(deftest test-download-variable
  (with-test-inventory
    (let [reference-time (date-time 2010 10 30 6)
          filename (variable-path nww3 htsgwsfc reference-time)
          dataset-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z"]
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))
               latest-reference-time (has-args [nww3] (returns reference-time))]
        (let [variable (download-variable nww3 htsgwsfc)]
          (is (isa? (class (:interval variable)) Interval))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable)))))
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))]
        (let [variable (download-variable nww3 htsgwsfc :reference-time reference-time)]
          (is (isa? (class (:interval variable)) Interval))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable))))))))

(deftest test-download-model
  (with-test-inventory
    (let [reference-time (date-time 2010 10 30 6)
          filename (variable-path nww3 htsgwsfc reference-time)]
      (expect [copy-dataset (returns filename)
               latest-reference-time (has-args [nww3] (returns reference-time))]
        (let [model (download-model nww3)]
          (is (= (:name nww3) (:name model)))
          (is (= (:description nww3) (:description model)))
          (is (= (:dods nww3) (:dods model)))
          (is (not (empty? (:variables model))))
          (is (isa? (class (:interval model)) Interval))
          (is (= reference-time (:reference-time model)))
          (is (= 0 (:size model))))))))

(deftest test-download-gfs
  (let [reference-time (date-time 2010 10 30 6)]
    (with-test-inventory
      (expect [latest-reference-time (returns reference-time)
               copy-dataset (has-args [] (returns ""))]
        (is (download-gfs))))))

(deftest test-download-wave-watch
  (let [reference-time (date-time 2010 10 30 6)]
    (with-test-inventory
      (expect [latest-reference-time (returns reference-time)
               copy-dataset (has-args [] (returns ""))]
        (is (download-wave-watch))))))
