(ns netcdf.test.repo
  (:use [netcdf.model :only (nww3)]
        clojure.test
        netcdf.repo))

(def test-repository (make-local-repository "/home/roman/.netcdf"))

(deftest test-make-local-repository
  (let [repository (make-local-repository "/home/roman/.netcdf")]
    (is (instance? netcdf.repo.LocalRepository repository))
    (is (= "/home/roman/.netcdf" (:url repository)))))

(deftest test-reference-times
  (let [reference-times (reference-times test-repository nww3)]
    reference-times))