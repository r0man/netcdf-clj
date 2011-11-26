(ns netcdf.test.forecast
  (:use [netcdf.model :only (global-forecast-system-models model? wave-watch-models)]
        [netcdf.variable :only (windsfc tmpsfc)]
        clojure.test
        netcdf.forecast))

(defforecast example-forecast
  "The example forecast."
  windsfc wave-watch-models
  tmpsfc global-forecast-system-models)

(deftest test-defforecast
  (is (= "example-forecast" (:name example-forecast)))
  (is (= "The example forecast." (:description example-forecast))))

(deftest test-make-forecast
  (let [forecast
        (make-forecast
         :name "example-forecast"
         :description "The example forecast.")]
    (is (= "example-forecast" (:name forecast)))
    (is (= "The example forecast." (:description forecast)))))

(deftest test-forecast-models
  (let [models (forecast-models example-forecast)]
    (is (= 7 (count models)))
    (is (every? model? models))))

(deftest test-forecast-variables
  (is (contains? (forecast-variables example-forecast) tmpsfc))
  (is (contains? (forecast-variables example-forecast) windsfc)))