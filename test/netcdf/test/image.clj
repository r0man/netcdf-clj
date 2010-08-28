(ns netcdf.test.image
  (:import java.awt.image.BufferedImage)
  (:use clojure.test netcdf.image))

(deftest test-make-buffered-image
  (let [image (make-buffered-image 20 10)]
    (is (isa? (class image) BufferedImage))
    (is (= (.getWidth image) 20))
    (is (= (.getHeight image) 10))
    (is (= (.getType image) BufferedImage/TYPE_INT_ARGB))))
