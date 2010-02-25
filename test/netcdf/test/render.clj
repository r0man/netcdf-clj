(ns netcdf.test.render
  (:import java.awt.Color)
  (:use clojure.test netcdf.render netcdf.location netcdf.test.helper))

(deftest test-water-color?
  (are [color expected]
    (is (= (water-color? color) expected))
    (Color. 152 178 203) true
    (Color. 153 178 203) true
    (Color. 153 179 203) true
    (Color. 153 179 204) true))

(deftest test-render-static-map
  (let [graphics (.getGraphics (make-buffered-image 500 250))
        map (render-static-map graphics (make-location 0 0) :width 500 :height 250)]
    (is (= (.getWidth map) 500))
    (is (= (.getHeight map) 250))))

(deftest test-render-datatype
  (let [graphics (.getGraphics (make-buffered-image 500 250))
        map (render-datatype graphics *datatype* *valid-time* (make-location 0 0) :width 10 :height 10)]
    (is (= (.getWidth map) 10))
    (is (= (.getHeight map) 10))))
