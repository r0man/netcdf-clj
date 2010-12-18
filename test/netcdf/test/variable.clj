(ns netcdf.test.variable
  (:use clojure.test
        netcdf.variable))

(deftest test-defvariable
  (defvariable dirpwsfc-example "Primary wave direction" :unit "°")
  (is (= "dirpwsfc-example" (:name dirpwsfc-example)))
  (is (= "Primary wave direction" (:description dirpwsfc-example)))
  (is (= "°" (:unit dirpwsfc-example))))
