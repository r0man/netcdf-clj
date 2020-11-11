(ns netcdf.dods-test
  (:import java.util.Calendar java.io.File java.net.URI)
  (:require [netcdf.model :refer [nww3 gfs-0p25]])
  (:use [clj-time.core :only (now date-time year minutes minus month day hour plus)]
        [netcdf.model :only (nww3)]
        clj-time.format
        clojure.test
        netcdf.dods
        netcdf.test
        netcdf.time))

(deftest test-datasources
  (with-test-inventory
    (let [sources (datasources nww3)]
      (is (= 2 (count sources)))
      (let [source (first sources)]
        (is (= "/wave/nww3/nww320101030/nww320101030_00z" (:name source)))
        (is (= "WAVE_nww3 Global wave model fcst from 00Z30oct2010, downloaded Oct 30 04:39 UTC" (:description source)))
        (is (= "https://nomads.ncep.noaa.gov/dods/wave/nww3/nww320101030/nww320101030_00z.das" (:das source)))
        (is (= "https://nomads.ncep.noaa.gov/dods/wave/nww3/nww320101030/nww320101030_00z.dds" (:dds source)))
        (is (= "https://nomads.ncep.noaa.gov/dods/wave/nww3/nww320101030/nww320101030_00z" (:dods source)))
        (is (= (date-time 2010 10 30) (:reference-time source)))))))

(deftest test-datasources-with-pattern
  (with-test-inventory
    (let [sources (datasources gfs-0p25)]
      (is (= 1 (count sources)))
      (let [source (first sources)]
        (is (=  (:name source) "/gfs_0p25/gfs20150224/gfs_0p25_00z"))
        (is (= (:description source)
               "GFS 0.25 deg starting from 00Z24feb2015, downloaded Feb 24 04:46 UTC"))
        (is (= (:das source)
               "https://nomads.ncep.noaa.gov/dods/gfs_0p25/gfs20150224/gfs_0p25_00z.das"))
        (is (= (:dds source)
               "https://nomads.ncep.noaa.gov/dods/gfs_0p25/gfs20150224/gfs_0p25_00z.dds"))
        (is (= (:dods source)
               "https://nomads.ncep.noaa.gov/dods/gfs_0p25/gfs20150224/gfs_0p25_00z"))
        (is (= (date-time 2015 02 24) (:reference-time source)))))))

(deftest test-datasource
  (with-test-inventory
    (let [source (datasource nww3 (date-time 2010 10 30))]
      (is (= "/wave/nww3/nww320101030/nww320101030_00z" (:name source)))
      (is (= "WAVE_nww3 Global wave model fcst from 00Z30oct2010, downloaded Oct 30 04:39 UTC" (:description source)))
      (is (= "https://nomads.ncep.noaa.gov/dods/wave/nww3/nww320101030/nww320101030_00z.das" (:das source)))
      (is (= "https://nomads.ncep.noaa.gov/dods/wave/nww3/nww320101030/nww320101030_00z.dds" (:dds source)))
      (is (= "https://nomads.ncep.noaa.gov/dods/wave/nww3/nww320101030/nww320101030_00z" (:dods source)))
      (is (= (date-time 2010 10 30) (:reference-time source))))
    (let [times (reference-times nww3)]
      (testing "first in inventory"
        (is (= (:reference-time (datasource nww3 (first times)))
               (first times))))
      (testing "second in inventory"
        (is (= (:reference-time (datasource nww3 (second times)))
               (second times))))
      (testing "last in inventory"
        (is (= (:reference-time (datasource nww3 (last times)))
               (last times))))
      (testing "one minute after first inventory"
        (is (= (:reference-time (datasource nww3 (plus (first times) (minutes 1))))
               (first times))))
      (testing "one minute after second inventory"
        (is (= (:reference-time (datasource nww3 (plus (second times) (minutes 1))))
               (second times))))
      (testing "one minute after last inventory"
        (is (= (:reference-time (datasource nww3 (plus (last times) (minutes 1))))
               (last times))))
      (testing "one minute before first inventory"
        (is (nil? (:reference-time (datasource nww3 (minus (first times) (minutes 1)))))))
      (testing "one minute before second inventory"
        (is (= (:reference-time (datasource nww3 (minus (second times) (minutes 1))))
               (first times))))
      (testing "one minute before last inventory"
        (is (= (:reference-time (datasource nww3 (minus (last times) (minutes 1))))
               (nth (seq times) (- (count times) 2)))))
      (testing "with time string"
        (is (= (:reference-time (datasource nww3 (format-time (first times))))
               (first times)))))))

(deftest test-datasource-with-pattern
  (with-test-inventory
    (let [source (datasource gfs-0p25 (date-time 2015 02 24))]
      (is (=  (:name source) "/gfs_0p25/gfs20150224/gfs_0p25_00z"))
      (is (= (:description source)
             "GFS 0.25 deg starting from 00Z24feb2015, downloaded Feb 24 04:46 UTC"))
      (is (= (:das source)
             "https://nomads.ncep.noaa.gov/dods/gfs_0p25/gfs20150224/gfs_0p25_00z.das"))
      (is (= (:dds source)
             "https://nomads.ncep.noaa.gov/dods/gfs_0p25/gfs20150224/gfs_0p25_00z.dds"))
      (is (= (:dods source)
             "https://nomads.ncep.noaa.gov/dods/gfs_0p25/gfs20150224/gfs_0p25_00z"))
      (is (= (date-time 2015 02 24) (:reference-time source))))))

(deftest test-reference-times
  (with-test-inventory
    (let [times (reference-times nww3)]
      (is (= (date-time 2010 10 30) (first times)))
      (is (= (date-time 2010 10 30 6) (last times))))))

(deftest test-parse-inventory
  (let [datasets (parse-inventory "test-resources/dods/xml")]
    (is (= 11 (count datasets)))
    (let [dataset (first datasets)]
      (is (= "/wave/akw/akw20101030/akw20101030_00z" (:name dataset)))
      (is (= "WAVE_AKW Regional Alaska Waters wave model fcst from 00Z30oct2010, downloaded Oct 30 04:28 UTC" (:description dataset)))
      (is (= "https://nomads.ncep.noaa.gov/dods/wave/akw/akw20101030/akw20101030_00z.das" (:das dataset)))
      (is (= "https://nomads.ncep.noaa.gov/dods/wave/akw/akw20101030/akw20101030_00z.dds" (:dds dataset)))
      (is (= "https://nomads.ncep.noaa.gov/dods/wave/akw/akw20101030/akw20101030_00z" (:dods dataset)))
      (is (= (date-time 2010 10 30) (:reference-time dataset))))))

(deftest test-parse-reference-time
  (let [url "https://nomad5.ncep.noaa.gov:9090/dods/waves/nww3/nww320090907/nww3_00z"]
    (let [time (parse-reference-time url)]
      (is (= time (date-time 2009 9 7 0 0 0))))
    (let [time (parse-reference-time (URI. url))]
      (is (= time (date-time 2009 9 7 0 0 0))))))
