(ns netcdf.test.variable
  (:import org.joda.time.Interval)
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
        clojure.test
        clojure.contrib.mock
        netcdf.test.helper
        netcdf.dods
        netcdf.repository
        netcdf.model
        netcdf.variable))

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
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))
               latest-reference-time (has-args [nww3] (returns reference-time))]
        (let [variable (download-variable nww3 htsgwsfc)]
          (is (isa? (class (:interval variable)) Interval))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable)))))
      (expect [copy-dataset (has-args [dataset-url filename ["htsgwsfc"]] (returns filename))]
        (let [variable (download-variable nww3 htsgwsfc :reference-time reference-time)]
          (is (isa? (class (:interval variable)) Interval))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable))))))))