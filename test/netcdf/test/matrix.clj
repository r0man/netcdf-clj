(ns netcdf.test.matrix
  (:import java.io.File incanter.Matrix)
  (:require [netcdf.geo-grid :as grid])
  (:use clojure.test
        incanter.core
        netcdf.matrix
        netcdf.test.helper))

(refer-private 'netcdf.matrix)

(def *matrix* (grid/read-matrix (grid/open-geo-grid *dataset-uri* *variable*)))

(deftest test-meta-data-filename
  (are [filename expected]
    (is (= (meta-data-filename filename) expected))
    "matrix" "matrix.meta"
    "matrix.json" "matrix.meta"
    "/tmp/matrix.json" "/tmp/matrix.meta"))

(deftest test-read-matrix
  (let [filename "/tmp/test-read-matrix"]
    (write-matrix *matrix* filename)
    (let [matrix (read-matrix filename)]
      (is (isa? (class matrix) Matrix))
      (is (= (.rows matrix) (.rows *matrix*)))
      (is (= (.columns matrix) (.columns *matrix*))))))

(deftest test-write-matrix
  (let [filename "/tmp/test-write-matrix"]
    (is (= (write-matrix *matrix* filename) filename))
    (is (.exists (File. filename)))))

(deftest test-read-meta-data
  (let [filename "/tmp/test-read-meta-data"]
    (write-meta-data *matrix* filename)
    (is (= (read-meta-data filename) (meta *matrix*)))))

(deftest test-write-meta-data
  (let [filename "/tmp/test-write-meta-data"]
    (is (= (write-meta-data *matrix* filename) filename))
    (is (.exists (File. filename)))))
