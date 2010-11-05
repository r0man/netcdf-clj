(ns netcdf.test.model
  (:require [netcdf.dods :as dods])
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
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

(deftest test-local-path
  (is (= (local-path (find-model-by-name "akw") "htsgwsfc" (date-time 2010 11 5 6))
         "./akw/htsgwsfc/20101105/06.nc"))
  (is (= (local-path (find-model-by-name "akw") "htsgwsfc" (date-time 2010 11 5 6) "/tmp")
         "/tmp/akw/htsgwsfc/20101105/06.nc")))

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
