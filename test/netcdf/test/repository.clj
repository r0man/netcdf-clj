(ns netcdf.test.repository
  (:use clojure.test
        netcdf.model
        netcdf.repository))

(deftest test-local-repository
  (let [repo (local-repository *root-dir*)]
    (is (= *root-dir* (:uri repo)))))
