(ns netcdf.test.datatype
  (:use clojure.test netcdf.datatype netcdf.location)
  (:require [netcdf.dataset :as dataset]))

(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
(def *variable* "htsgwsfc")

(def *datatype* (make-datatype *dataset-uri* *variable*))
(def *valid-time* (first (valid-times *datatype*)))

(defn make-example-datatype []
  (make-datatype *dataset-uri* *variable*))

(deftest test-make-datatype
  (let [datatype (make-datatype *dataset-uri* *variable*)]
    (is (= (:dataset-uri datatype)) *dataset-uri*)
    (is (= (:variable datatype) *variable*))
    (is (nil? (:service datatype)))))

(deftest test-open-datatype
  (let [datatype (open-datatype (make-datatype *dataset-uri* *variable*))]
    (is (= (:dataset-uri datatype)) *dataset-uri*)
    (is (= (:variable datatype) *variable*))
    (is (= (class (:service datatype)) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-datatype-open?
  (let [datatype (make-example-datatype)]
    (is (not (datatype-open? datatype-open?)))
    (is (datatype-open? (open-datatype datatype)))))

(deftest test-read-at-location
  (let [datatype (open-datatype (make-example-datatype))
        valid-time (first (valid-times datatype))
        location (make-location 0 0)
        record (read-at-location datatype valid-time location)]
    (is (= (:actual-location record) (make-location 0 0 0)))
    (is (= (:distance record) 0))
    (is (= (:requested-location record) location))
    (is (= (:valid-time record) valid-time))
    (is (= (:variable record) *variable*))))

(deftest test-read-datatype
  (let [datatype (open-datatype (make-example-datatype))
        valid-time (first (valid-times datatype))
        records (read-datatype datatype valid-time)]
    (is (> (count records) 0))))

(deftest test-valid-times-with-closed-datatype
  (let [valid-times (valid-times *datatype*)]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))

(deftest test-valid-times-with-open-datatype
  (let [valid-times (valid-times (open-datatype *datatype*))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))
