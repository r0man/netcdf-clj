(ns netcdf.test.repository
  (:use clojure.test
        netcdf.model
        netcdf.repository))

(deftest test-local-repository
  (let [repo (local-repository *root-dir*)]
    (is (= *root-dir* (:uri repo)))))

(deftest test-netcdf-file-seq
  (let [files (netcdf-file-seq "/home/roman/.netcdf")]
    (is (every? #(isa? (class %) java.io.File) files))
    (is (every? #(.endsWith (str %) ".nc") files))))
