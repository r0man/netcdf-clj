(ns netcdf.test.render
  (:import java.awt.Color)
  (:use clojure.test netcdf.render))

(deftest test-water-color?
  (are [color expected]
    (is (= (water-color? color) expected))
    (Color. 152 178 203) true
    (Color. 153 179 203) true
    (Color. 153 179 204) true))
