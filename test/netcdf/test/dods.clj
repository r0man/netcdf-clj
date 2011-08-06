(ns netcdf.test.dods
  (:import java.util.Calendar java.io.File java.net.URI)
  (:use [clj-time.core :only (date-time year month day hour)]
        clj-time.format
        netcdf.dods
        netcdf.model
        netcdf.variable
        netcdf.test.helper
        clojure.test
        clojure.contrib.mock))

(deftest test-current-reference-time
  (is (current-reference-time nww3)))

(deftest test-dods-repository
  (let [repo (dods-repository "http://nomads.ncep.noaa.gov:9090/dods/xml")]
    (is (= "http://nomads.ncep.noaa.gov:9090/dods/xml" (:uri repo)))))

(deftest test-find-datasets-by-url
  (with-test-inventory
    (is (= 2 (count (find-datasets-by-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"))))))

(deftest test-find-datasets-by-url-and-reference-time
  (with-test-inventory
    (let [datasets (find-datasets-by-url-and-reference-time "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3" (date-time 2010 10 30 0))]
      (is (= 1 (count datasets)))
      (let [dataset (first datasets)]
        (is (= (:dods dataset) "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_00z"))
        (is (= (date-time 2010 10 30) (:reference-time dataset)))))
    (is (= (find-datasets-by-url-and-reference-time "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3" (date-time 2010 10 30 0))
           (find-datasets-by-url-and-reference-time "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3" "2010-10-30T00:00:00Z")))))

(deftest test-find-inventory-by-url
  (is (= (find-inventory-by-url "test-resources/dods/wave/akw")
         (find-inventory-by-url "test-resources/dods/wave/nww3")))
  (let [datasets (find-inventory-by-url "test-resources/dods/wave/akw")]
    (is (= 9 (count datasets)))
    (let [dataset (first datasets)]
      (is (= "/wave/akw/akw20101030/akw20101030_00z" (:name dataset)))
      (is (= "WAVE_AKW Regional Alaska Waters wave model fcst from 00Z30oct2010, downloaded Oct 30 04:28 UTC" (:description dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw/akw20101030/akw20101030_00z.das" (:das dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw/akw20101030/akw20101030_00z.dds" (:dds dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw/akw20101030/akw20101030_00z" (:dods dataset)))
      (is (= (date-time 2010 10 30) (:reference-time dataset))))))

(deftest test-inventory-url
  (are [url expected]
    (is (= (inventory-url url) expected))
    "http://nomads.ncep.noaa.gov:9090/dods/wave/akw" "http://nomads.ncep.noaa.gov:9090/dods/xml"
    "file:/home/roman/workspace/netcdf-clj/test-resources/dods/wave/akw" "file:/home/roman/workspace/netcdf-clj/test-resources/dods/xml"))

(deftest test-latest-reference-time
  (with-test-inventory
    (is (= (latest-reference-time nww3) (date-time 2010 10 30 6)))))

(deftest test-parse-inventory
  (let [datasets (parse-inventory "test-resources/dods/xml")]
    (is (= 9 (count datasets)))
    (let [dataset (first datasets)]
      (is (= "/wave/akw/akw20101030/akw20101030_00z" (:name dataset)))
      (is (= "WAVE_AKW Regional Alaska Waters wave model fcst from 00Z30oct2010, downloaded Oct 30 04:28 UTC" (:description dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw/akw20101030/akw20101030_00z.das" (:das dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw/akw20101030/akw20101030_00z.dds" (:dds dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/wave/akw/akw20101030/akw20101030_00z" (:dods dataset)))
      (is (= (date-time 2010 10 30) (:reference-time dataset))))))

(deftest test-parse-reference-time
  (let [url "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3/nww320090907/nww3_00z"]
    (let [time (parse-reference-time url)]
      (is (= time (date-time 2009 9 7 0 0 0))))
    (let [time (parse-reference-time (URI. url))]
      (is (= time (date-time 2009 9 7 0 0 0))))))
