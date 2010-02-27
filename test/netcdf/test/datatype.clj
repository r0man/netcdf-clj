(ns netcdf.test.datatype
  (:use clojure.test incanter.core incanter.datasets netcdf.datatype netcdf.location)
  (:require [netcdf.dataset :as dataset]))

(def *dataset-uri* "/home/roman/.weather/20100215/akw.06.nc")
(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
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

(deftest test-description
  (is (= (description (open-example-datatype)) "** surface sig height of wind waves and swell [m]")))

(deftest test-lat-axis
  (let [axis (lat-axis (open-example-datatype))]
    (is (= (:lat-min axis) -78))
    (is (= (:lat-max axis) 78))
    (is (= (:lat-size axis) 157))
    (is (= (:lat-step axis) 1))))

(deftest test-lon-axis
  (let [axis (lon-axis (open-example-datatype))]
    (is (= (:lon-min axis) 0))
    (is (= (:lon-max axis) 358.75))
    (is (= (:lon-size axis) 288))
    (is (= (:lon-step axis) 1.25))))

(deftest test-axis
  (let [datatype (open-example-datatype)]
    (is (= (axis datatype) (merge (lat-axis datatype) (lon-axis datatype))))))

(deftest test-make-datatype
  (let [datatype (make-datatype *dataset-uri* *variable*)]
    (is (= (:dataset-uri datatype)) *dataset-uri*)
    (is (= (:variable datatype) *variable*))
    (is (nil? (:service datatype)))))

(deftest test-open-datatype
  (let [datatype (open-example-datatype)]
    (is (= (:dataset-uri datatype)) *dataset-uri*)
    (is (= (:variable datatype) *variable*))
    (is (= (class (:service datatype)) ucar.nc2.dt.grid.GeoGrid))
    (is (= (select-keys datatype (keys (lat-axis datatype))) (lat-axis datatype)))
    (is (= (select-keys datatype (keys (lon-axis datatype))) (lon-axis datatype)))))

(deftest test-read-seq
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        sequence (read-seq datatype valid-time (make-location 0 0))]
    (is (seq? sequence))
    (is (= (count sequence) 4))
    (is (= (:actual-location (nth sequence 0)) (make-location 0 0)))    
    (is (= (:actual-location (nth sequence 1)) (make-location 0 (:lon-step datatype))))
    (is (= (:actual-location (nth sequence 2)) (make-location (* -1 (:lat-step datatype)) 0)))
    (is (= (:actual-location (nth sequence 3)) (make-location (* -1 (:lat-step datatype)) (:lon-step datatype))))
    (let [m (meta sequence)]
      (is (= (:description m) (description datatype)))
      (is (= (:valid-time m) valid-time))
      (is (= (:variable m) (:variable datatype)))
      (is (= (:lat-max m) 0))
      (is (= (:lat-min m) (* -1 (:lat-step datatype))))
      (is (= (:lat-size m) 2))
      (is (= (:lat-step m) (:lat-step datatype)))
      (is (= (:lon-max m) (:lon-step datatype)))
      (is (= (:lon-min m) 0))
      (is (= (:lon-size m) 2))
      (is (= (:lon-step m) (:lon-step datatype))))))

(deftest test-read-matrix
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        sequence (read-seq datatype valid-time (make-location 0 0))
        matrix (read-matrix datatype valid-time (make-location 0 0))]
    (is (matrix? matrix))
    (is (= (count sequence) 4))
    (is (= (count matrix) 2))
    (is (every? #(= % 2) (map count matrix)))    
    (is (= (sel matrix 0 0) (:value (nth sequence 0))))
    (is (= (sel matrix 0 1) (:value (nth sequence 1))))
    (is (= (sel matrix 1 0) (:value (nth sequence 2))))
    (is (= (sel matrix 1 1) (:value (nth sequence 3))))
    (let [m (meta matrix)]
      (is (= (:description m) (description datatype)))
      (is (= (:valid-time m) valid-time))
      (is (= (:variable m) (:variable datatype)))
      (is (= (:lat-max m) 0))
      (is (= (:lat-min m) (* -1 (:lat-step datatype))))
      (is (= (:lat-size m) 2))
      (is (= (:lat-step m) (:lat-step datatype)))
      (is (= (:lon-max m) (:lon-step datatype)))
      (is (= (:lon-min m) 0))
      (is (= (:lon-size m) 2))
      (is (= (:lon-step m) (:lon-step datatype))))))

(deftest test-read-at-location
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))]
    (let [data (read-at-location datatype valid-time (make-location 0 0))]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 0 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*)))
    (let [data (read-at-location datatype valid-time (make-location 78 0))]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 78 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*))
      (is (.isNaN (:value data))))
    (let [data (read-at-location datatype valid-time (make-location 78 0) :nil -999)]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 78 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*))
      (is (= (:value data) -999)))))

(deftest test-time-index
  (let [datatype (open-example-datatype)]
    (is (= (time-index datatype (first (valid-times datatype))) 0))
    (is (= (time-index datatype (last (valid-times datatype))) (- (count (valid-times datatype)) 1)))))

(deftest test-valid-times-with-closed-datatype
  (let [valid-times (valid-times *datatype*)]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))

(deftest test-valid-times-with-open-datatype
  (let [valid-times (valid-times (open-datatype *datatype*))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))
