(ns netcdf.test.interpolation
  (:import (javax.media.jai InterpolationBicubic InterpolationBilinear))
  (:use [incanter.core :only (matrix)]
        clojure.test
        netcdf.geo-grid
        netcdf.interpolation
        netcdf.location
        netcdf.test.helper))

(def *coord-system* (coord-system *geo-grid*))

(def matrix-2x2 (matrix [[6 7] [10 11]]))
(def matrix-4x4 (matrix [[1 2 3 4] [5 6 7 8] [9 10 11 12] [13 14 15 16]]))

(deftest test-interpolate-bilinear-2x2
  (are [x-fract y-fract expected]
    (is (= (interpolate matrix-2x2 x-fract y-fract) expected))
    0 0 6 ; central sample
    0.2 0 6.200000002980232
    0.7 0 6.699999988079071
    1.0 0 7
    0.5 0.5 8.5))

(deftest test-interpolate-bilinear-4x4
  (are [x-fract y-fract expected]
    (is (= (interpolate matrix-4x4 x-fract y-fract) expected))
    0 0 6 ; central sample
    0.2 0 6.200000002980232
    0.7 0 6.699999988079071
    1.0 0 7
    0.5 0.5 8.5))

;;; TODO: wtf?
(deftest test-interpolate-bicubic-2x2
  (with-interpolation (InterpolationBicubic. 8)
    ;; (println matrix-2x2)
    (are [x-fract y-fract expected]
      (is (= (interpolate matrix-2x2 x-fract y-fract) expected))
      0 0 11 ; central sample ???
      1 1 2.90625
      )))

(deftest test-sample-offsets
  (is (= (sample-offsets)
         (sample-offsets 2)
         (sample-offsets 2 2)))
  (are [width height expected]
    (is (= expected (sample-offsets width height)))
    2 2 [[0 0] [0 1] [1 0] [1 1]]
    4 4 [[0 0] [0 1] [0 2] [0 3] [1 0] [1 1] [1 2] [1 3] [2 0] [2 1] [2 2] [2 3] [3 0] [3 1] [3 2] [3 3]]
    4 2 [[0 0] [0 1] [1 0] [1 1] [2 0] [2 1] [3 0] [3 1]]))

(deftest test-sample-location
  (are [lat lon lat-step lon-step expect-lat expect-lon]
    (is (= (sample-location (make-location lat lon) lat-step lon-step)
           (make-location expect-lat expect-lon)))
    0 0 1 1 0 0
    0 0 1 1 0 0
    77.0 0 1 1.25 77 0
    77.1 0 1 1.25 78 0
    77.9 0 1 1.25 78 0
    77 0.0 1 1.25 77 0
    77 0.1 1 1.25 77 0
    77 0.9 1 1.25 77 0))

(deftest test-sample-locations
  (let [sample (sample-locations *coord-system* (make-location 78 0))]
    (is (= (make-location 78 0) (nth sample 0)))
    (is (= (make-location 78 1.25) (nth sample 1)))
    (is (= (make-location 77 0) (nth sample 2)))
    (is (= (make-location 77 1.25) (nth sample 3))))
  (let [sample (sample-locations *coord-system* (make-location 77 1.25))]
    (is (= (make-location 77 1.25) (nth sample 0)))
    (is (= (make-location 77 2.5) (nth sample 1)))
    (is (= (make-location 76 1.25) (nth sample 2)))
    (is (= (make-location 76 2.5) (nth sample 3))))
  (let [sample (sample-locations *coord-system* (make-location 77.5 0.625))]
    (is (= (make-location 78 0) (nth sample 0)))
    (is (= (make-location 78 1.25) (nth sample 1)))
    (is (= (make-location 77 0) (nth sample 2)))
    (is (= (make-location 77 1.25) (nth sample 3)))))

;; (deftest test-interpolate-bilinear-4x4
;;   (println (interpolate matrix-4x4 0 0))
;;   )

;; (def *dataset-uri* "/home/roman/.weather/20100215/akw.06.nc")
;; (def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
;; (def *variable* "htsgwsfc")

;; (def *datatype* (make-datatype *dataset-uri* *variable*))
;; (def *valid-time* (first (valid-times *datatype*)))

;; (defn make-example-datatype []
;;   (make-datatype *dataset-uri* *variable*))

;; (defn open-example-datatype []
;;   (open-datatype (make-example-datatype)))

;; (deftest test-read-sample-2x2
;;   (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
;;     (let [sample (read-sample-2x2 datatype valid-time (make-location 78 0))]
;;       (is (= sample (read-matrix datatype valid-time (make-location 78 0) :width 2 :height 2)))
;;       (is (= (:x-fract (meta sample)) 0))
;;       (is (= (:y-fract (meta sample)) 0)))
;;     (let [sample (read-sample-2x2 datatype valid-time (make-location 78 0) :nil 0)]
;;       (is (= sample (read-matrix datatype valid-time (make-location 78 0) :width 2 :height 2 :nil 0)))
;;       (is (= (:x-fract (meta sample)) 0))
;;       (is (= (:y-fract (meta sample)) 0)))))

;; (deftest test-read-sample-4x4
;;   (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
;;     (let [sample (read-sample-4x4 datatype valid-time (make-location 77 1.25))]
;;       (is (= sample (read-matrix datatype valid-time (make-location 78 0) :width 4 :height 4)))
;;       (is (= (:x-fract (meta sample)) 0.3333333333333333))
;;       (is (= (:y-fract (meta sample)) 0.3333333333333333)))
;;     (let [sample (read-sample-4x4 datatype valid-time (make-location 77 1.25) :nil 0)]
;;       (is (= sample (read-matrix datatype valid-time (make-location 78 0) :width 4 :height 4 :nil 0))))))

;; (deftest test-with-meta+
;;   (let [obj [1 2] m {:key "val"}]
;;     (is (= (with-meta+ obj {}) obj))
;;     (is (= (with-meta+ obj m) obj))
;;     (is (= (meta (with-meta+ obj m)) m))
;;     (is (= (meta (with-meta+ (with-meta obj {:key "x"}) m)) m))
;;     (is (= (meta (with-meta+ (with-meta obj m) {:key2 "val2"})) (merge m {:key2 "val2"})))))

;; (deftest test-x-fract
;;   (let [sample (with-meta [] {:lon-min 0.0 :lon-max 1.25})]
;;     (are [lat lon fract]
;;       (is (= (x-fract sample (make-location lat lon)) fract))
;;       0 0 0
;;       0 1.25 1
;;       0 -0.1 -0.08
;;       0 0.1 0.08
;;       0 0.124 0.0992
;;       0 1.26 1.008)))

;; (deftest test-y-fract
;;   (let [sample (with-meta [] {:lat-min 76 :lat-max 77})]
;;     (are [lat lon fract]
;;       (is (= (y-fract sample (make-location lat lon)) fract))
;;       77 1.25 0
;;       76 1.25 1
;;       77.1 1.25 -0.09999999999999432
;;       76.9 1.25 0.09999999999999432
;;       76.1 1.25 0.9000000000000057)))

;; (deftest test-interpolate-bicubic-2x2
;;   (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
;;     (interpolate-bicubic-2x2 datatype valid-time (make-location 77 0))))

;; (deftest test-interpolate-bilinear-2x2
;;   (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
;;     (interpolate-bilinear-2x2 datatype valid-time (make-location 77 0))))

;; (deftest test-interpolate-bilinear-4x4
;;   (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
;;     (interpolate-bilinear-4x4 datatype valid-time (make-location 77 0))))
