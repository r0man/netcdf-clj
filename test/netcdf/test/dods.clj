(ns netcdf.test.dods
  (:import java.util.Calendar java.io.File)
  (:use clojure.test
        clojure.contrib.str-utils
        netcdf.dods
        incanter.chrono))

(def *repository* (make-repository "akw" "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw" "Alaskan Waters"))

(def *dods* "file:///home/roman/workspace/netcdf-clj/test/fixtures/dods.xml")
(def *time* (date 2010 3 25 6 0 0)) ;;; CHECK UTC !!!

(deftest test-inventory-url
  (is (= (inventory-url *repository*) (str (:root *repository*) "/xml"))))

(deftest test-latest-reference-time
  (is (= (latest-reference-time *repository*)
         (last (reference-times *repository*)))))

(deftest test-make-repository
  (let [repository (make-repository "akw" "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw" "Alaskan Waters")]
    (is (= (:name repository ) "akw"))
    (is (= (:description repository ) "Alaskan Waters"))
    (is (= (:root repository ) "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw"))))

(deftest test-parse-dods
  (let [dods (parse-dods *dods*)]
    (is (= (first dods) "http://nomad5.ncep.noaa.gov:9090/dods/aofs/ofs"))
    (is (= (last dods) "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3/nww320091124/nww3_12z"))))

(deftest test-parse-reference-times
  (let [times (parse-reference-times *dods* "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3")]
    (is (= (first times) (date 2009 9 7 0 0 0)))
    (is (= (last times) (date 2009 11 24 12 0 0)))))

(deftest test-dataset-directory
  (is (= (dataset-directory *repository* *time*)
         (str (:name *repository*) (format-date *time* "yyyyMMdd")))))

(deftest test-dataset-filename
  (is (= (dataset-filename *repository* *time*)
         (str (:name *repository*) "_" (format-date *time* "HH") "z"))))

(deftest test-dataset-url
  (is (= (dataset-url *repository* *time*)
         (str "http://nomad5.ncep.noaa.gov:9090/dods/waves/"
              (:name *repository*) "/"
              (dataset-directory *repository* *time*) "/"
              (dataset-filename *repository* *time*)))))

(deftest test-dataset-url->time
  (let [time (dataset-url->time "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3/nww320090907/nww3_00z")]
    (is (= time (date 2009 9 7 0 0 0)))))

(deftest test-reference-times
  (let [reference-times (reference-times *repository*)]
    (is (not (empty? reference-times)))))

(deftest test-latest-reference-time
  (is (not (nil? (latest-reference-time *repository*)))))

(deftest test-valid-time->reference-time
  (are [valid-time reference-time]
    (is (= (valid-time->reference-time valid-time) reference-time))
    (date 2010 3 25 0 0) (date 2010 3 25 0 0)
    (date 2010 3 25 5 59) (date 2010 3 25 0 0)
    (date 2010 3 25 6 0) (date 2010 3 25 6 0)
    (date 2010 3 25 11 59) (date 2010 3 25 6 0)
    (date 2010 3 25 12 0) (date 2010 3 25 12 0)))
