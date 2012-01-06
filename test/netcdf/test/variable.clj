(ns netcdf.test.variable
  (:import org.joda.time.DateTime org.joda.time.Interval)
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
        clojure.test
        netcdf.test.helper
        netcdf.repository
        [netcdf.model :exclude (valid-times)]
        netcdf.variable))

(def example-time (date-time 2011 12 1 6))

(deftest test-make-variable
  (let [variable (make-variable :name "dirpwsfc" :description "Primary wave direction" :unit "°")]
    (is (variable? variable))
    (is (= "dirpwsfc" (:name variable)))
    (is (= "Primary wave direction" (:description variable)))
    (is (= "°" (:unit variable)))))

(deftest test-variable
  (is (nil? (variable nil)))
  (is (nil? (variable "")))
  (is (= htsgwsfc (variable :htsgwsfc))))

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
          (is (instance? Interval (:interval variable)))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable)))))
      (with-redefs [copy-dataset (constantly filename)]
        (let [variable (download-variable nww3 htsgwsfc :reference-time reference-time)]
          (is (instance? Interval (:interval variable)))
          (is (= filename (:filename variable)))
          (is (= 0 (:size variable)))
          (is (= reference-time (:reference-time variable))))))))

(deftest test-valid-times
  (with-test-inventory
    (with-redefs [variable-path (constantly "/tmp/netcdf-test.nc")]
      (let [valid-times (valid-times nww3 htsgwsfc)]
        (is (every? #(instance? DateTime %) valid-times))
        (is (= 61 (count valid-times)))))))

(deftest test-variable-fragment
  (is (= (str "nww3/htsgwsfc/2011/12/01/060000Z.nc")
         (variable-fragment nww3 htsgwsfc example-time))))
