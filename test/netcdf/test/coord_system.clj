(ns netcdf.test.coord-system
  (:import ucar.unidata.geoloc.Projection)
  (:require [netcdf.geo-grid :as grid])
  (:use clojure.test netcdf.coord-system netcdf.test.helper))

(def *coord-system* (grid/coord-system (grid/open-geo-grid *dataset-uri* *variable*)))

(deftest test-projection
  (is (isa? (class (projection *coord-system*)) Projection)))

(deftest test-x-y-index
  (is (= (x-y-index *coord-system* {:latitude 0 :longitude 0}) [0 78])))
