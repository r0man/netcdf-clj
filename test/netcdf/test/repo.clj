(ns netcdf.test.repo
  (:use [clj-time.core :only (date-time)]
        [netcdf.model :only (nww3)]
        [netcdf.variable :only (htsgwsfc)]
        clojure.test
        netcdf.repo))

(def example-repository (make-local-repository))
(def example-time (date-time 2011 12 1 6))

(deftest test-make-local-repository
  (let [repository (make-local-repository *local-root*)]
    (is (instance? netcdf.repo.LocalRepository repository))
    (is (= *local-root* (:url repository)))))

(deftest test-reference-times
  (let [reference-times (reference-times example-repository nww3)]
    reference-times))

(deftest test-variable-url
  (is (= (str (:url example-repository) "/nww3/htsgwsfc/2011/12/01/060000Z.nc")
         (variable-url example-repository nww3 htsgwsfc example-time))))
