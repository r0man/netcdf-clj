(ns netcdf.test.datatype
  (:use clojure.test netcdf.datatype netcdf.location)
  (:require [netcdf.dataset :as dataset]))

(def *dataset-uri* "/home/roman/.weather/20100215/akw.06.nc")
(def *variable* "htsgwsfc")

(def *datatype* (make-datatype *dataset-uri* *variable*))
(def *valid-time* (first (valid-times *datatype*)))

(defn make-example-datatype []
  (make-datatype *dataset-uri* *variable*))

(defn open-example-datatype []
  (open-datatype (make-example-datatype)))

(deftest test-bounding-box
  (let [bounds (bounding-box (open-example-datatype))]
    (is (= (class bounds) ucar.unidata.geoloc.LatLonRect))))

(deftest test-datatype-open?
  (let [datatype (make-example-datatype)]
    (is (not (datatype-open? datatype-open?)))
    (is (datatype-open? (open-datatype datatype)))))

(deftest test-latitude-range
  (let [range (latitude-range (open-example-datatype))]
    (is (= (count range) 122))
    (is (= (first range) 44.75))
    (is (= (last range) 75.0))))

(deftest test-longitude-range
  (let [range (longitude-range (open-example-datatype))]
    (is (= (count range) 154))
    (is (= (first range) -20.5))
    (is (= (last range) 56.0))))

(deftest test-make-datatype
  (let [datatype (make-datatype *dataset-uri* *variable*)]
    (is (= (:dataset-uri datatype)) *dataset-uri*)
    (is (= (:variable datatype) *variable*))
    (is (nil? (:service datatype)))))

(deftest test-open-datatype
  (let [datatype (open-example-datatype)]
    (is (= (:dataset-uri datatype)) *dataset-uri*)
    (is (= (:variable datatype) *variable*))
    (is (= (class (:service datatype)) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-read-datatype
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        records (read-datatype datatype valid-time)]
    (is (> (count records) 0))))

(deftest test-read-at-location
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        location (make-location 0 0)
        record (read-at-location datatype valid-time location)]
    (is (location? (:actual-location record)))
    (is (>= (:distance record) 0))
    (is (= (:requested-location record) location))
    (is (= (:valid-time record) valid-time))
    (is (= (:variable record) *variable*))))

(deftest test-valid-times-with-closed-datatype
  (let [valid-times (valid-times *datatype*)]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))

(deftest test-valid-times-with-open-datatype
  (let [valid-times (valid-times (open-datatype *datatype*))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))
