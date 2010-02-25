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

;; (deftest test-read-matrix
;;   (let [datatype (open-example-datatype)
;;         valid-time (first (valid-times datatype))
;;         data (read-matrix datatype valid-time)]
;;     (is (= (class data) incanter.Matrix))
;;     (is (= (count data) (:size (lon-axis datatype))))
;;     (let [m (meta data)]
;;       (is (= (:description m) (description datatype)))
;;       (is (= (:lat-axis m) (lat-axis datatype)))
;;       (is (= (:lon-axis m) (lon-axis datatype)))
;;       (is (= (:valid-time m) valid-time))
;;       (is (= (:variable m) (:variable datatype))))))

(deftest test-read-seq
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        data (read-seq datatype valid-time)]
    (is (seq? data))
    ;; (is (= (count data) 19065))
    (let [m (meta data)]
      (is (= (:description m) (description datatype)))
      (is (= (:valid-time m) valid-time))
      (is (= (:variable m) (:variable datatype)))
      (is (= (select-keys m (keys (axis datatype))) (axis datatype))))))

(deftest test-read-at-location
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        location (make-location 0 0)
        record (read-at-location datatype valid-time location)]
    (is (location? (:actual-location record)))
    (is (= (:requested-location record) location))
    (is (= (:valid-time record) valid-time))
    (is (= (:variable record) *variable*))))

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

