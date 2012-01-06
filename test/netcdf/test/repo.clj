(ns netcdf.test.repo
  (:import ucar.nc2.dt.grid.GeoGrid
           org.joda.time.DateTime)
  (:require [netcdf.geo-grid :as grid])
  (:use [clj-time.core :only (date-time)]
        [netcdf.model :only (nww3)]
        [netcdf.variable :only (htsgwsfc)]
        clojure.test
        netcdf.repo
        netcdf.test.helper))

(def example-repository (make-local-repository))
(def example-time (date-time 2011 12 1 6))

(deftest test-make-local-repository
  (let [repository (make-local-repository *local-root*)]
    (is (instance? netcdf.repo.LocalRepository repository))
    (is (= *local-root* (:url repository)))))

(deftest test-local-variable-url
  (is (= (str (:url example-repository) "/nww3/htsgwsfc/2011/12/01/060000Z.nc")
         (local-variable-url example-repository nww3 htsgwsfc example-time))))

(deftest test-open-grid
  (with-repository (make-local-repository)
    (let [grid (open-grid nww3 htsgwsfc example-reference-time)]
      (is (instance? GeoGrid grid))
      (is (= (:name htsgwsfc) (.getName grid)))
      (is (= example-reference-time (first (grid/valid-times grid))))))
  (with-repository (make-dods-repository)
    (let [grid (open-grid nww3 htsgwsfc example-reference-time)]
      (is (instance? GeoGrid grid))
      (is (= (:name htsgwsfc) (.getName grid)))
      (is (= example-reference-time (first (grid/valid-times grid)))))))

(deftest test-reference-times
  (with-repository (make-dods-repository)
    (let [reference-times (reference-times nww3)]
      (is (not (empty? reference-times)))
      (is (every? #(instance? DateTime %1) reference-times))))
  (with-repository (make-local-repository)
    (let [reference-times (reference-times nww3)]
      (is (not (empty? reference-times)))
      (is (every? #(instance? DateTime %1) reference-times)))))
