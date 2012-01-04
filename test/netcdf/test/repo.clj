(ns netcdf.test.repo
  (:use clojure.test
        netcdf.repo))

(deftest test-make-local-repository
  (let [repository (make-local-repository "/home/roman/.netcdf")]
    (is (instance? netcdf.repo.LocalRepository repository))))