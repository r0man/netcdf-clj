(ns netcdf.test.forecast
  (:use clojure.test
        netcdf.forecast
        netcdf.model
        netcdf.variable))

(defforecast example-forecast
  "The example forecast."
  windsfc wave-watch-models
  tmpsfc global-forecast-system-models)

(deftest test-defforecast
  (is (= "example-forecast" (:name example-forecast)))
  (is (= "The example forecast." (:description example-forecast))))

(deftest test-make-forecast
  (let [forecast (make-forecast
                  :name "example-forecast"
                  :description "The example forecast.")]
    (is (= "example-forecast" (:name forecast)))
    (is (= "The example forecast." (:description forecast)))))

(deftest test-variables
  (is (contains? (variables example-forecast) tmpsfc))
  (is (contains? (variables example-forecast) windsfc)))