(ns netcdf.test.render
  (:import java.awt.Color java.io.File)
  (:use clojure.test netcdf.render netcdf.location netcdf.test.helper))

;; (deftest test-water-color?
;;   (are [color]
;;     (is (water-color? color))
;;     (Color. 152 178 203)
;;     (Color. 153 178 203)
;;     (Color. 153 179 203)
;;     (Color. 153 179 204)))

;; (deftest test-datatype-image
;;   (let [image (datatype-image *datatype* :width 20 :height 10)]
;;     (is (= (.getWidth image) 20))
;;     (is (= (.getHeight image) 10))))

;; (deftest test-render-static-map
;;   (let [image (make-buffered-image 20 10)]
;;     (render-static-map (.getGraphics image) (make-location 0 0) :width 20 :height 10)))

;; (deftest test-render-datatype
;;   (let [image (make-buffered-image 20 10)]
;;     (render-datatype (.getGraphics image) (read-matrix *datatype*) :width 20 :height 10)))

;; (deftest test-write-datatype-image
;;   (let [file (File. "/tmp/save-datatype-image.png")]
;;     (is (= (write-datatype-image file *datatype* :width 20 :height 10) file))))

;; (deftest test-save-datatype-image
;;   (let [filename "/tmp/save-datatype-image.png"]
;;     (is (= (save-datatype-image filename *datatype* :width 20 :height 10) filename))))

;; (deftest test-image-coords->location
;;   (let [center {:latitude 0 :longitude 0}]
;;     (are [width height zoom x y latitude longitude]
;;       (is (= (image-coords->location center x y width height zoom) {:latitude latitude :longitude longitude}))
;;       256 256 0 0 0 85.05112877980659 -180
;;       256 256 0 1 0 85.05112877980659 -178.59375
;;       256 256 0 128 0 85.05112877980659 0
;;       256 256 0 256 0 85.05112877980659 -180
;;       256 256 0 0 1 84.92832092949963 -180
;;       256 256 0 0 128 0 -180
;;       256 256 0 0 256 -85.05112877980659 -180
;;       256 256 1 0 0 66.51326044311185 -90
;;       256 256 1 1 0 66.51326044311185 -89.29687500000006
;;       256 256 1 128 0 66.51326044311185 0
;;       256 256 1 256 0 66.51326044311185 90
;;       256 256 1 0 1 66.23145747862573 -90
;;       256 256 1 0 128 0 -90
;;       256 256 1 0 256 -66.51326044311185 -90
;;       512 256 2 0 0 40.979898069620134 -90
;;       512 256 2 512 0 40.979898069620134 90
;;       512 256 2 256 128 0 0
;;       512 256 2 0 256 -40.979898069620134 -90
;;       512 256 2 512 256 -40.979898069620134 90)))


