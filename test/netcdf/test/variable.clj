(ns netcdf.test.variable
  (:import org.joda.time.Interval)
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
        clojure.test
        netcdf.test.helper
        netcdf.dods
        netcdf.repository
        netcdf.model
        netcdf.variable))

(deftest test-make-variable
  (let [variable (make-variable :name "dirpwsfc" :description "Primary wave direction" :unit "°")]
    (is (variable? variable))
    (is (= "dirpwsfc" (:name variable)))
    (is (= "Primary wave direction" (:description variable)))
    (is (= "°" (:unit variable)))))

(deftest test-variable?
  (is (not (variable? nil)))
  (is (not (variable? "")))
  (is (variable? htsgwsfc)))

(deftest test-defvariable
  (defvariable dirpwsfc-example "Primary wave direction" :unit "°")
  (is (= "dirpwsfc-example" (:name dirpwsfc-example)))
  (is (= "Primary wave direction" (:description dirpwsfc-example)))
  (is (= "°" (:unit dirpwsfc-example))))

(deftest test-download-variable
  (with-test-inventory
    (let [reference-time (date-time 2010 10 30 6)
          filename (variable-path nww3 htsgwsfc reference-time)
          dataset-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z"]
      (with-redefs [copy-dataset (constantly filename)
                    latest-reference-time (constantly reference-time)]
        (let [variable (download-variable nww3 htsgwsfc)]
          (is (isa? (class (:interval variable)) Interval))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable)))))
      (with-redefs [copy-dataset (constantly filename)]
        (let [variable (download-variable nww3 htsgwsfc :reference-time reference-time)]
          (is (isa? (class (:interval variable)) Interval))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable))))))))