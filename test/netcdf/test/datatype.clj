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

;; (deftest test-latitude-axis
;;   (let [axis (latitude-axis (open-example-datatype))]
;;     (is (= (:min axis) 44.75))
;;     (is (= (:max axis) 75.25))
;;     (is (= (:size axis) 123))
;;     (is (= (:step axis) 0.25))))

;; (deftest test-longitude-axis
;;   (let [axis (longitude-axis (open-example-datatype))]
;;     (is (= (:min axis) 159.5))
;;     (is (= (:max axis) 236.5))
;;     (is (= (:size axis) 155))
;;     (is (= (:step axis) 0.5))))

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

(deftest test-read-matrix
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        data (read-matrix datatype valid-time)]
    (is (= (class data) incanter.Matrix))
    (is (= (count data) (:size (longitude-axis datatype))))
    (let [m (meta data)]
      (is (= (:description m) (description datatype)))
      (is (= (:latitude-axis m) (latitude-axis datatype)))
      (is (= (:longitude-axis m) (longitude-axis datatype)))
      (is (= (:valid-time m) valid-time))
      (is (= (:variable m) (:variable datatype))))))

(deftest test-read-seq
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        data (read-seq datatype valid-time)]
    (is (seq? data))
    ;; (is (= (count data) 19065))
    (let [m (meta data)]
      (is (= (:description m) (description datatype)))
      (is (= (:latitude-axis m) (latitude-axis datatype)))
      (is (= (:longitude-axis m) (longitude-axis datatype)))
      (is (= (:valid-time m) valid-time))
      (is (= (:variable m) (:variable datatype))))))

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

(deftest test-central-sample-location
  (is (= (central-sample-location (make-location 0 0) 1 1) (make-location 0 0)))
  (is (= (central-sample-location (make-location 77.1 0.2) 1 1) (make-location 77 0))))

(deftest test-sample-latitude
  (are [latitude height step expected]
    (is (= (sample-latitude latitude height step) expected))
    77 0 1 '()
    77 1 1 '(77)
    77 2 1 '(76 77)
    77.5 0 1 '()
    77.5 1 1 '(77)
    77.5 2 1 '(76 77)))

(deftest test-sample-longitude
  (are [longitude width step expected]
    (is (= (sample-longitude longitude width step) expected))
    0 0 1.25 '()
    0 1 1.25 '(0)
    0 2 1.25 '(0 1.25)
    0.5 0 1.25 '()
    0.5 1 1.25 '(0)
    0.5 2 1.25 '(0 1.25)))

(deftest test-sample-location
  (are [lat lon expected]
    (is (= (map location->array (sample-location (make-location lat lon) :step-lat 1 :step-lon 1.25 :width 2 :height 2)) expected))
    76.9 0 [[76 0] [76 1.25] [75 0] [75 1.25]]
    77.0 0 [[77 0] [77 1.25] [76 0] [76 1.25]]
    77.5 0 [[77 0] [77 1.25] [76 0] [76 1.25]]
    78.0 0 [[78 0] [78 1.25] [77 0] [77 1.25]]))

(deftest test-location->sample-2x2
  (let [locations (location->sample-2x2 (make-location 77 0) :step-lat 1 :step-lon 1.25)]
    (are [key latitude longitude]
      (is (= (key locations) (make-location latitude longitude)))
      :s00 77 0
      :s01 77 1.25
      :s10 76 0
      :s11 76 1.25)))

;; (deftest test-read-sample-2x2
;;   (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
;;     (println (read-at-location datatype valid-time (make-location 77 0)))
;;     (println (read-sample-2x2 datatype valid-time (make-location 77 0)))

;;     ))

;; (deftest test-sample-location-4x4
;;   (let [locations (location->sample-4x4 (make-location 77 1.25) :step-lat 1 :step-lon 1.25)]
;;     (are [key latitude longitude]
;;       (is (= (key locations) (make-location latitude longitude)))
;;       :s__ 78 0
;;       :s_0 78 1.25
;;       :s_1 78 2.5
;;       :s_2 78 3.75
;;       :s0_ 77 0
;;       :s00 77 1.25
;;       :s01 77 2.5
;;       :s02 77 3.75
;;       :s1_ 76 0
;;       :s10 76 1.25
;;       :s11 76 2.5
;;       :s12 76 3.75
;;       :s2_ 75 0
;;       :s20 75 1.25
;;       :s21 75 2.5
;;       :s22 75 3.75
;;       )))

;; (sample-location (make-location 0 0) 1 1 0 0)

