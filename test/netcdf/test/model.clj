(ns netcdf.test.model
  (:require [netcdf.dods :as dods])
  (:use [clj-time.core :only (date-time)]
        clojure.test
        clojure.contrib.mock
        netcdf.model
        netcdf.test.helper))

(deftest test-model?
  (is (not (model? "")))
  (is (not (model? nil)))
  (is (model? (make-model "test" "http://example.com" "Test Model"))))

(deftest test-make-model
  (let [model (make-model "akw" "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw" "Regional Alaska Waters Wave Model")]
    (is (model? model))
    (is (= (:name model) "akw"))
    (is (= (:description model) "Regional Alaska Waters Wave Model"))
    (is (= (:url model) "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw"))))

(deftest test-find-model-by-name
  (let [model (make-model "test" "http://example.com" "Test Model")]    
    (is (nil? (find-model-by-name (:name model))))
    (register-model model)
    (is (= (find-model-by-name (:name model)) model))
    (unregister-model model)
    (is (every? #(model? (find-model-by-name %))
                ["akw" "enp" "nah" "nph" "nww3" "wna" "gfs-hd"]))))

(deftest test-register-model
  (let [model (make-model "test" "http://example.com" "Test Model")]    
    (is (= (register-model model) model))
    (is (= (find-model-by-name (:name model)) model))
    (unregister-model model)))

(deftest test-unregister-model
  (let [model (make-model "test" "http://example.com" "Test Model")]    
    (register-model model)
    (is (= (unregister-model model) model))
    (is (nil? (find-model-by-name (:name model))))))

(deftest test-reference-times  
  (with-test-inventory
    (let [reference-times (reference-times (find-model-by-name "nww3"))]
      (is (= 2 (count reference-times))))))

(deftest test-latest-reference-time
  (with-test-inventory
    (is (= (latest-reference-time (find-model-by-name "nww3"))
           (date-time 2010 10 30 6)))))
