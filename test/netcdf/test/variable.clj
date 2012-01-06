(ns netcdf.test.variable
  (:import org.joda.time.DateTime org.joda.time.Interval)
  (:use [clj-time.core :only (date-time)]
        [netcdf.dataset :only (copy-dataset)]
        [netcdf.model :exclude (valid-times)]
        [netcdf.repository :only (local-dataset-url)]
        clojure.test
        netcdf.test.helper
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
    (let [filename (local-dataset-url nww3 htsgwsfc example-reference-time)
          dataset-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320101030/nww320101030_06z"]
      (with-redefs [copy-dataset (constantly filename)
                    netcdf.repository/reference-times (constantly [example-reference-time])]
        (let [variable (download-variable nww3 htsgwsfc)]
          (is (instance? Interval (:interval variable)))
          (is (= filename (:filename variable)))
          (is (< 0 (:size variable)))
          (is (= example-reference-time (:reference-time variable)))))
      (with-redefs [copy-dataset (constantly filename)]
        (let [variable (download-variable nww3 htsgwsfc :reference-time example-reference-time)]
          (is (instance? Interval (:interval variable)))
          (is (= filename (:filename variable)))
          (is (< 0 (:size variable)))
          (is (= example-reference-time (:reference-time variable))))))))

(deftest test-valid-times
  (let [valid-times (valid-times nww3 htsgwsfc example-reference-time)]
    (is (not (empty? valid-times)))
    (is (= 61 (count valid-times)))
    (is (every? #(instance? DateTime %) valid-times)))
  (with-test-inventory
    (with-redefs [netcdf.repository/reference-times (constantly [example-reference-time])]
      (let [valid-times (valid-times nww3 htsgwsfc)]
        (is (not (empty? valid-times)))
        (is (= 61 (count valid-times)))
        (is (every? #(instance? DateTime %) valid-times))))))

(deftest test-variable-fragment
  (is (= (str "nww3/htsgwsfc/2011/12/01/060000Z.nc")
         (variable-fragment nww3 htsgwsfc example-time))))
