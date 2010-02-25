(ns netcdf.test.interpolation
  (:use clojure.test netcdf.datatype netcdf.interpolation netcdf.location))

;; (def *dataset-uri* "/home/roman/.weather/20100215/akw.06.nc")
(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
(def *variable* "htsgwsfc")

(def *datatype* (make-datatype *dataset-uri* *variable*))
(def *valid-time* (first (valid-times *datatype*)))

(defn make-example-datatype []
  (make-datatype *dataset-uri* *variable*))

(defn open-example-datatype []
  (open-datatype (make-example-datatype)))

(deftest test-central-sample-location
  (are [lat lon lat-step lon-step expect-lat expect-lon]
    (is (= (central-sample-location (make-location lat lon) lat-step lon-step) (make-location expect-lat expect-lon)))
    0 0 1 1 0 0
    0 0 1 1 0 0
    77.0 0 1 1.25 77 0
    77.1 0 1 1.25 78 0
    77.9 0 1 1.25 78 0
    77 0.0 1 1.25 77 0
    77 0.1 1 1.25 77 0
    77 0.9 1 1.25 77 0
    ))

(deftest test-read-sample-2x2
  (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
    (read-sample-2x2 datatype valid-time (make-location 77 0))))

;; (deftest test-sample-latitude
;;   (are [latitude height step expected]
;;     (is (= (sample-latitude latitude height step) expected))
;;     77 0 1 '()
;;     77 1 1 '(77)
;;     77 2 1 '(76 77)
;;     77.5 0 1 '()
;;     77.5 1 1 '(77)
;;     77.5 2 1 '(76 77)))

;; (deftest test-sample-longitude
;;   (are [longitude width step expected]
;;     (is (= (sample-longitude longitude width step) expected))
;;     0 0 1.25 '()
;;     0 1 1.25 '(0)
;;     0 2 1.25 '(0 1.25)
;;     0.5 0 1.25 '()
;;     0.5 1 1.25 '(0)
;;     0.5 2 1.25 '(0 1.25)))

;; (deftest test-sample-location
;;   (are [lat lon expected]
;;     (is (= (map location->array (sample-location (make-location lat lon) :lat-step 1 :lon-step 1.25 :width 2 :height 2)) expected))
;;     76.9 0 [[77 0] [77 1.25] [76 0] [76 1.25]]
;;     77.0 0 [[77 0] [77 1.25] [76 0] [76 1.25]]
;;     77.5 0 [[78 0] [78 1.25] [77 0] [77 1.25]]
;;     78.0 0 [[78 0] [78 1.25] [77 0] [77 1.25]]))

;; (deftest test-location->sample-2x2
;;   (let [locations (location->sample-2x2 (make-location 77 0) :lat-step 1 :lon-step 1.25)]
;;     (are [key latitude longitude]
;;       (is (= (key locations) (make-location latitude longitude)))
;;       :s00 77 0
;;       :s01 77 1.25
;;       :s10 76 0
;;       :s11 76 1.25))
;;   (let [locations (location->sample-2x2 (make-location 77.5 1) :lat-step 1 :lon-step 1.25)]
;;     (are [key latitude longitude]
;;       (is (= (key locations) (make-location latitude longitude)))
;;       :s00 78 0
;;       :s01 78 1.25
;;       :s10 77 0
;;       :s11 77 1.25)))

;; (deftest test-sample-location-4x4
;;   (let [locations (location->sample-4x4 (make-location 77 1.25) :lat-step 1 :lon-step 1.25)]
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

