(ns netcdf.test.render
  (:import java.awt.Color)
  (:use clojure.test netcdf.render netcdf.location netcdf.test.helper))

(deftest test-water-color?
  (are [color]
    (is (water-color? color))
    (Color. 152 178 203)
    (Color. 153 178 203)
    (Color. 153 179 203)
    (Color. 153 179 204)))

(deftest test-render-static-map
  (let [map (render-static-map (make-buffered-image 500 250) (make-location 0 0) :width 500 :height 250)]
    (is (= (.getWidth map) 500))
    (is (= (.getHeight map) 250))))

(deftest test-render-datatype
  (let [component (make-buffered-image 10 10)]
    (render-datatype component *datatype* *valid-time* (make-location 0 0) :width 10 :height 10)))

(deftest test-read-datatype-image
  (let [image (read-datatype-image *datatype* *valid-time* (make-location 0 0) :width 10 :height 10)]
    (is (= (.getWidth image) 10))
    (is (= (.getHeight image) 10))))

(deftest test-save-datatype-image
  (let [filename "/tmp/save-datatype-image.png"
        image (save-datatype-image filename *datatype* *valid-time* (make-location 0 0) :width 10 :height 10)]
    (is (= (.getWidth image) 10))
    (is (= (.getHeight image) 10))))

(deftest test-file-extension
  (is (nil? (file-extension "filename")))
  (is (= (file-extension "image.png") "png")))

