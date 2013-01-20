(ns netcdf.test.matrix
  (:import java.awt.image.BufferedImage java.io.File)
  (:require [netcdf.geo-grid :as grid])
  (:use clojure.test
        incanter.core
        netcdf.matrix
        netcdf.test.helper))

(refer-private 'netcdf.matrix)

(def example-matrix (grid/read-matrix (grid/open-geo-grid example-path example-variable)))

(deftest test-meta-data-filename
  (are [filename expected]
    (is (= (meta-data-filename filename) expected))
    "matrix" "matrix.meta"
    "matrix.json" "matrix.meta"
    "/tmp/matrix.json" "/tmp/matrix.meta"))

(deftest test-write-matrix
  (let [filename "/tmp/test-write-matrix"]
    (is (= (write-matrix example-matrix filename) filename))
    (is (.exists (File. filename)))))

(deftest test-read-meta-data
  (let [filename "/tmp/test-read-meta-data"]
    (write-meta-data example-matrix filename)
    (is (= (read-meta-data filename) (meta example-matrix)))))

(deftest test-write-meta-data
  (let [filename "/tmp/test-write-meta-data"]
    (is (= (write-meta-data example-matrix filename) filename))
    (is (.exists (File. filename)))))

(deftest test-print-matrix
  (let [writer (java.io.StringWriter.)]
    (print-matrix (matrix [[1 2 3] [4 5 6]]) writer)
    (is (= "#incanter/matrix ((1.0 2.0 3.0) (4.0 5.0 6.0))" (str writer))))
  (let [writer (java.io.StringWriter.)]
    (print-matrix (matrix [[1 2 3] [4 5 Double/NaN]]) writer)
    (is (= "#incanter/matrix ((1.0 2.0 3.0) (4.0 5.0 nil))" (str writer)))))

(deftest test-print-dup-matrix
  (let [writer (java.io.StringWriter.)]
    (print-dup (matrix [[1 2 3] [4 5 6]]) writer)
    (is (= "#incanter/matrix ((1.0 2.0 3.0) (4.0 5.0 6.0))" (str writer)))))

(deftest test-print-method-matrix
  (let [writer (java.io.StringWriter.)]
    (print-method (matrix [[1 2 3] [4 5 6]]) writer)
    (is (= "#incanter/matrix ((1.0 2.0 3.0) (4.0 5.0 6.0))" (str writer)))))

(deftest test-read-incanter-matrix
  (is (= (matrix [[1 2 3] [4 5 6]])
         (read-incanter-matrix '((1.0 2.0 3.0) (4.0 5.0 6.0))))))

(deftest test-read-string
  (is (= (matrix [[1 2 3] [4 5 6]])
         (read-string "#incanter/matrix ((1.0 2.0 3.0) (4.0 5.0 6.0))")))
  (is (= (matrix [[1 2 3] [4 5 Double/NaN]])
         (read-string "#incanter/matrix ((1.0 2.0 3.0) (4.0 5.0 nil))"))))
