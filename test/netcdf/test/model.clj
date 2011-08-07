(ns netcdf.test.model
  (:import java.io.File java.net.URI org.joda.time.Interval)
  (:require [netcdf.dods :as dods])
  (:use [clj-time.coerce :only (to-long)]
        [clj-time.core :only (date-time interval plus minutes minus)]
        [netcdf.dataset :only (copy-dataset)]
        clojure.contrib.mock
        clojure.test
        netcdf.model
        netcdf.dods
        netcdf.repository
        netcdf.test.helper
        netcdf.time
        netcdf.variable))

(def *akw*
  (make-model
   :description "Regional Alaska Waters Wave Model"
   :name "akw"
   :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
   :variables wave-watch-variables))

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

(deftest test-find-dataset
  (with-test-inventory
    (let [dataset (find-dataset nww3)]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z")))
    (let [dataset (find-dataset nww3 (date-time 2010 10 30 0))]
      (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_00z")))))

(deftest test-find-reference-time
  (with-test-inventory
    (let [reference-times (reference-times nww3)]
      (testing "first in inventory"
        (is (= (find-reference-time nww3 (first reference-times))
               (first reference-times))))
      (testing "second in inventory"
        (is (= (find-reference-time nww3 (second reference-times))
               (second reference-times))))
      (testing "last in inventory"
        (is (= (find-reference-time nww3 (last reference-times))
               (last reference-times))))
      (testing "one minute after first inventory"
        (is (= (find-reference-time nww3 (plus (first reference-times) (minutes 1)))
               (first reference-times))))
      (testing "one minute after second inventory"
        (is (= (find-reference-time nww3 (plus (second reference-times) (minutes 1)))
               (second reference-times))))
      (testing "one minute after last inventory"
        (is (= (find-reference-time nww3 (plus (last reference-times) (minutes 1)))
               (last reference-times))))
      (testing "one minute before first inventory"
        (is (nil? (find-reference-time nww3 (minus (first reference-times) (minutes 1))))))
      (testing "one minute before second inventory"
        (is (= (find-reference-time nww3 (minus (second reference-times) (minutes 1)))
               (first reference-times))))
      (testing "one minute before last inventory"
        (is (= (find-reference-time nww3 (minus (last reference-times) (minutes 1)))
               (nth reference-times (- (count reference-times) 2)))))
      (testing "with time string"
        (is (= (find-reference-time nww3 (format-time (first reference-times)))
               (first reference-times)))))))

(deftest test-make-model
  (let [model *akw*]
    (is (= "akw" (:name model)))
    (is (= "Regional Alaska Waters Wave Model" (:description model)))
    (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw" (:dods model)))
    (is (= wave-watch-variables (:variables model)))))

(deftest test-model
  (is (nil? (model "unknown")))
  (is (= akw (model (:name akw))))
  (is (= akw (model (keyword (:name akw)))))
  (let [model (model "akw")]
    (is (= "akw" (:name model)))
    (is (= 11 (count (:variables model)))))
  (let [model (model "akw" ["htsgwsfc" "windsfc"])]
    (is (= "akw" (:name model)))
    (is (= 2 (count (:variables model))))))

(deftest test-model?
  (is (not (model? nil)))
  (is (not (model? "")))
  (is (model? nww3)))

(deftest test-reference-times
  (with-test-inventory
    (let [reference-times (reference-times nww3)]
      (is (< (to-long (first reference-times))
             (to-long (second reference-times))))
      (is (= 2 (count reference-times))))))

(deftest test-variable-path
  (is (= (str *repository* "/htsgwsfc/2010/11/05/060000Z/akw.nc")
         (variable-path akw htsgwsfc "2010-11-05T06:00:00Z")))
  (is (= (str *repository* "/htsgwsfc/2010/11/05/060000Z/akw.nc")
         (variable-path akw htsgwsfc (date-time 2010 11 5 6))))
  (with-repository "/tmp"
    (is (= "/tmp/htsgwsfc/2010/11/05/060000Z/akw.nc"
           (variable-path akw htsgwsfc (date-time 2010 11 5 6)))))
  (with-repository "s3n://burningswell/netcdf"
    (is (= "s3n://burningswell/netcdf/htsgwsfc/2010/11/05/060000Z/akw.nc"
           (variable-path akw htsgwsfc (date-time 2010 11 5 6))))))
