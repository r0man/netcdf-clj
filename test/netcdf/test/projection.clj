(ns netcdf.test.projection
  (:import ucar.unidata.geoloc.projection.LatLonProjection)
  (:require [netcdf.geo-grid :as grid])
  (:use clojure.test
        netcdf.location
        netcdf.projection
        netcdf.projection-point
        netcdf.test.helper))

(def *grid* (grid/open-geo-grid example-path example-variable))

(def *projection* (.. *grid* getCoordinateSystem getProjection))

(deftest test-backward-mapping
  (are [x y longitude latitude]
    (is (= (backward-mapping *projection* (make-projection-point x y))
           (make-location latitude longitude)))
    0 0 0 0))

(deftest test-forward-mapping
  (are [longitude latitude x y]
    (is (= (forward-mapping *projection* (make-location latitude longitude))
           (make-projection-point x y)))
    0 0 0 0))

;; (deftest test-location->xy
;;   (are [latitude longitude x y]
;;     (is (= (location->xy *projection* (make-location latitude longitude)) [x y]))
;;     0 0 0 0))

;; (deftest test-location->row-column
;;   (are [latitude longitude row column]
;;     (is (= (location->row-column *projection* (make-location latitude longitude)) [row column]))
;;     0 0 0 0))
