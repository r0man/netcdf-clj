(ns netcdf.test.repo
  (:use [clj-time.core :only (date-time)]
        [netcdf.model :only (nww3)]
        [netcdf.variable :only (htsgwsfc)]
        clojure.test
        netcdf.repo))

(def example-repository (make-local-repository "/home/roman/.netcdf"))
(def example-time (date-time 2011 12 1 6))

(deftest test-make-local-repository
  (let [repository (make-local-repository "/home/roman/.netcdf")]
    (is (instance? netcdf.repo.LocalRepository repository))
    (is (= "/home/roman/.netcdf" (:url repository)))))

(deftest test-local-reference-times
  (let [reference-times (local-reference-times example-repository nww3)]
    reference-times))

(deftest test-local-variable-url
  (is (= (str *local-root* "/nww3/htsgwsfc/2011/12/01/060000Z.nc")
         (variable-url example-repository nww3 htsgwsfc example-time))))
