(ns netcdf.test.matrix
  (:import java.awt.image.BufferedImage
           java.io.File incanter.Matrix)
  (:require [netcdf.geo-grid :as grid])
  (:use clojure.test
        incanter.core
        netcdf.image
        netcdf.matrix
        netcdf.model
        netcdf.test.helper
        netcdf.variable))

(refer-private 'netcdf.matrix)

(def *matrix* (grid/read-matrix (grid/open-geo-grid example-path example-variable)))

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

(deftest test-make-image
  (let [matrix (make-image *matrix*)]
    (is (isa? (class matrix) BufferedImage))))

(deftest test-render-image
  (let [image (make-buffered-image (ncol *matrix*) (nrow *matrix*))]
    (is (= (render-image *matrix* image) image))))

(deftest test-save-as-image
  (let [image (save-as-image *matrix* "/tmp/test-save-as-image.png")]
    (is (isa? (class image) BufferedImage))))
