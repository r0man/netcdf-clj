(ns netcdf.test.file
  (:import java.io.File)
  (:use clojure.test
        netcdf.file
        netcdf.model
        netcdf.test.helper))

(deftest test-netcdf-file?
  (is (netcdf-file? *dataset-uri*))
  (is (netcdf-file? (File. *dataset-uri*)))
  (is (not (netcdf-file? "test")))
  (is (not (netcdf-file? (File. "test")))))

(deftest test-netcdf-file-seq
  (let [files (netcdf-file-seq *root-dir*)]
    (is (every? #(isa? (class %) File) files))
    (is (every? netcdf-file? files))))