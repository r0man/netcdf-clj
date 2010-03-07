(ns netcdf.test.render
  (:import java.awt.Color java.io.File)
  (:use clojure.test netcdf.datatype netcdf.render netcdf.location netcdf.test.helper))

(deftest test-water-color?
  (are [color]
    (is (water-color? color))
    (Color. 152 178 203)
    (Color. 153 178 203)
    (Color. 153 179 203)
    (Color. 153 179 204)))

(deftest test-datatype-image
  (let [image (datatype-image *datatype* :width 20 :height 10)]
    (is (= (.getWidth image) 20))
    (is (= (.getHeight image) 10))))

(deftest test-render-static-map
  (let [image (make-buffered-image 20 10)]
    (render-static-map (.getGraphics image) (make-location 0 0) :width 20 :height 10)))

(deftest test-render-datatype
  (let [image (make-buffered-image 20 10)]
    (render-datatype (.getGraphics image) (read-matrix *datatype*) :width 20 :height 10)))

(deftest test-write-datatype-image
  (let [file (File. "/tmp/save-datatype-image.png")]
    (is (= (write-datatype-image file *datatype* :width 20 :height 10) file))))

(deftest test-save-datatype-image
  (let [filename "/tmp/save-datatype-image.png"]
    (is (= (save-datatype-image filename *datatype* :width 20 :height 10) filename))))
