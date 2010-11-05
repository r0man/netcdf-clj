(ns netcdf.test.repository
  (:require [netcdf.dods :as dods])
  (:use [clj-time.core :only (date-time)]
        clojure.test
        clojure.contrib.mock
        netcdf.repository
        netcdf.test.helper))

(deftest test-repository?
  (is (not (repository? "")))
  (is (not (repository? nil)))
  (is (repository? (make-repository "test" "http://example.com" "Test Model"))))

(deftest test-make-repository
  (let [repository (make-repository "akw" "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw" "Regional Alaska Waters Wave Model")]
    (is (repository? repository))
    (is (= (:name repository) "akw"))
    (is (= (:description repository) "Regional Alaska Waters Wave Model"))
    (is (= (:url repository) "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw"))))

(deftest test-find-repository-by-name
  (let [repository (make-repository "test" "http://example.com" "Test Model")]    
    (is (nil? (find-repository-by-name (:name repository))))
    (register-repository repository)
    (is (= (find-repository-by-name (:name repository)) repository))
    (unregister-repository repository)
    (is (every? #(repository? (find-repository-by-name %))
                ["akw" "enp" "nah" "nph" "nww3" "wna" "gfs-hd"]))))

(deftest test-register-repository
  (let [repository (make-repository "test" "http://example.com" "Test Model")]    
    (is (= (register-repository repository) repository))
    (is (= (find-repository-by-name (:name repository)) repository))
    (unregister-repository repository)))

(deftest test-unregister-repository
  (let [repository (make-repository "test" "http://example.com" "Test Model")]    
    (register-repository repository)
    (is (= (unregister-repository repository) repository))
    (is (nil? (find-repository-by-name (:name repository))))))

(deftest test-reference-times  
  (with-test-inventory
    (let [reference-times (reference-times (find-repository-by-name "nww3"))]
      (is (= 2 (count reference-times))))))

(deftest test-latest-reference-time
  (with-test-inventory
    (is (= (latest-reference-time (find-repository-by-name "nww3"))
           (date-time 2010 10 30 6)))))
