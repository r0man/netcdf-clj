(ns netcdf.test.geo-grid
  (:use clojure.test incanter.core incanter.datasets netcdf.geo-grid netcdf.location)
  (:require [netcdf.dataset :as dataset]))

(def *dataset-uri* "/home/roman/.weather/20100215/akw.06.nc")
(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
(def *variable* "htsgwsfc")

(defn open-example-geo-grid []
  (open-geo-grid *dataset-uri* *variable*))

(deftest test-open-geo-grid
  (let [geo-grid (open-example-geo-grid)]
    (is (= (class geo-grid) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-bounding-box
  (let [bounds (bounding-box (open-example-geo-grid))]
    (is (= (class bounds) ucar.unidata.geoloc.LatLonRect))))

(deftest test-coord-system
  (let [coord-system (coord-system (open-example-geo-grid))]
    (is (isa? (class coord-system) ucar.nc2.dt.grid.GridCoordSys))))

(deftest test-description
  (is (= (description (open-example-geo-grid)) "** surface sig height of wind waves and swell [m]")))

(deftest test-latitude-axis
  (let [axis (latitude-axis (open-example-geo-grid))]
    (is (= (:lat-min axis) -78))
    (is (= (:lat-max axis) 78))
    (is (= (:lat-size axis) 157))
    (is (= (:lat-step axis) 1))))

(deftest test-longitude-axis
  (let [axis (longitude-axis (open-example-geo-grid))]
    (is (= (:lon-min axis) 0))
    (is (= (:lon-max axis) 358.75))
    (is (= (:lon-size axis) 288))
    (is (= (:lon-step axis) 1.25))))

(deftest test-projection
  (let [projection (projection (open-example-geo-grid))]
    (is (isa? (class projection) ucar.unidata.geoloc.ProjectionImpl))))

(deftest test-lat-lon-axis
  (let [geo-grid (open-example-geo-grid)]
    (is (= (lat-lon-axis geo-grid) (merge (latitude-axis geo-grid) (longitude-axis geo-grid))))))

(deftest test-read-seq
  (let [geo-grid (open-example-geo-grid)
        valid-time (first (valid-times geo-grid))
        sequence (read-seq geo-grid)]
    (is (seq? sequence))
    (is (= (count sequence) 45216))
    (let [m (meta sequence)]
      (is (= (:valid-time m) valid-time))
      (is (= (:lat-max m) 78))
      (is (= (:lat-min m) -78))
      (is (= (:lat-step m) 1))      
      (is (= (:lon-max m) 358.75))
      (is (= (:lon-min m) 0))
      (is (= (:lon-step m) 1.25)))))

(deftest test-read-matrix
  (let [geo-grid (open-example-geo-grid)
        matrix (read-matrix geo-grid)]
    (is (matrix? matrix))
    (is (= (count matrix) 157))
    (is (every? #(= % 288) (map count matrix)))    
    (let [m (meta matrix)]
      (is (= (:valid-time m) (first (valid-times geo-grid))))
      (is (= (:lat-max m) 78))
      (is (= (:lat-min m) -78))
      (is (= (:lat-step m) 1))      
      (is (= (:lon-max m) 358.75))
      (is (= (:lon-min m) 0))
      (is (= (:lon-step m) 1.25)))))

(deftest test-time-index-with-geo-grid
  (let [geo-grid (open-example-geo-grid)]
    (is (= (time-index geo-grid (first (valid-times geo-grid))) 0))
    (is (= (time-index geo-grid (last (valid-times geo-grid))) (- (count (valid-times geo-grid)) 1)))))

(deftest test-valid-times
  (let [valid-times (valid-times (open-example-geo-grid))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))

;; (deftest test-read-matrix
;;   (let [geo-grid (open-example-geo-grid)
;;         sequence (read-seq geo-grid)
;;         matrix (read-matrix geo-grid)]
;;     (is (matrix? matrix))
;;     (is (= (count sequence) 4))
;;     (is (= (count matrix) 2))
;;     (is (every? #(= % 2) (map count matrix)))    
;;     (is (= (sel matrix 0 0) (:value (nth sequence 0))))
;;     (is (= (sel matrix 0 1) (:value (nth sequence 1))))
;;     (is (= (sel matrix 1 0) (:value (nth sequence 2))))
;;     (is (= (sel matrix 1 1) (:value (nth sequence 3))))
;;     (let [m (meta matrix)]
;;       (is (= (:description m) (description geo-grid)))
;;       (is (= (:valid-time m) (first (valid-times geo-grid))))
;;       (is (= (:variable m) (:variable geo-grid)))
;;       (is (= (:lat-max m) 0))
;;       (is (= (:lat-min m) (* -1 (:lat-step geo-grid))))
;;       (is (= (:lat-size m) 2))
;;       (is (= (:lat-step m) (:lat-step geo-grid)))
;;       (is (= (:lon-max m) (:lon-step geo-grid)))
;;       (is (= (:lon-min m) 0))
;;       (is (= (:lon-size m) 2))
;;       (is (= (:lon-step m) (:lon-step geo-grid))))))

;; (deftest test-read-matrix
;;   (let [geo-grid (open-example-geo-grid)
;;         sequence (read-seq geo-grid)
;;         matrix (read-matrix geo-grid :width 5 :height 5)]
;;     (is (matrix? matrix))
;;     (is (= (count sequence) 4))
;;     (is (= (count matrix) 5))
;;     (is (every? #(= % 5) (map count matrix)))    
;;     (is (= (sel matrix 0 0) (:value (nth sequence 0))))
;;     (is (= (sel matrix 0 1) (:value (nth sequence 1))))
;;     (is (= (sel matrix 1 0) (:value (nth sequence 2))))
;;     (is (= (sel matrix 1 1) (:value (nth sequence 3))))
;;     (let [m (meta matrix)]
;;       (is (= (:description m) (description geo-grid)))
;;       (is (= (:valid-time m) (first (valid-times geo-grid))))
;;       (is (= (:variable m) (:variable geo-grid)))
;;       (is (= (:lat-max m) 77))
;;       (is (= (:lat-min m) 73))
;;       (is (= (:lat-size m) 5))
;;       (is (= (:lat-step m) (:lat-step geo-grid)))
;;       (is (= (:lon-max m) 5))
;;       (is (= (:lon-min m) 0))
;;       (is (= (:lon-size m) 5))
;;       (is (= (:lon-step m) (:lon-step geo-grid))))))

;; (deftest test-read-datapoint-with-geo-grid
;;   (let [geo-grid (open-example-geo-grid)
;;         valid-time (first (valid-times geo-grid))]
;;     (is (read-datapoint geo-grid (make-location 0 0) :valid-time valid-time))
;;     (is (= -999 (read-datapoint geo-grid (make-location 78 0) :valid-time valid-time :nil -999)))))

;; (deftest test-read-datapoint-with-matrix
;;   (let [geo-grid (open-example-geo-grid)
;;         matrix (read-matrix geo-grid)
;;         valid-time (first (valid-times geo-grid))]
;;     (is (read-datapoint matrix (make-location 0 0) :valid-time valid-time))
;;     (is (= -999 (read-datapoint matrix (make-location 78 0) :valid-time valid-time :nil -999)))))




;; (deftest test-geo-grid-subset
;;   (let [geo-grid (open-example-geo-grid) valid-time (first (valid-times geo-grid))]
;;     (let [subset (geo-grid-subset geo-grid valid-time (make-location 0 0))]
;;       (is (= (:dataset-uri subset)) (:dataset-uri geo-grid))
;;       (is (= (:variable subset) (:variable geo-grid)))
;;       (is (not (= (:service subset) (:service geo-grid))))
;;       ;; (is (= (:lat-min subset) 0))
;;       ;; (is (= (:lat-max subset) 78))
;;       ;; (is (= (:lat-size subset) 3))
;;       (is (= (:lon-min subset) 0))
;;       (is (= (:lon-max subset) 1.25))
;;       (is (= (:lon-size subset) 2)))
;;     (let [subset (geo-grid-subset geo-grid valid-time (make-location 78 0) :width 2 :height 3)]
;;       (is (= (:dataset-uri subset)) (:dataset-uri geo-grid))
;;       (is (= (:variable subset) (:variable geo-grid)))
;;       (is (not (= (:service subset) (:service geo-grid))))
;;       (is (= (:lat-min subset) 76))
;;       (is (= (:lat-max subset) 78))
;;       (is (= (:lat-size subset) 3))
;;       (is (= (:lon-min subset) 0))
;;       (is (= (:lon-max subset) 1.25))
;;       (is (= (:lon-size subset) 2)))))

;; (deftest test-location->index
;;   (let [geo-grid (open-example-geo-grid)]
;;     (are [latitude longitude x y]
;;       (is (= (location->index geo-grid (make-location latitude longitude)) {:x x :y y}))
;;       0 0 0 78
;;       78 -180 144 156
;;       78 180 144 156
;;       78 179 143 156
;;       -78 180 144 0
;;       -78 179 143 0
;;       90 0 0 -1
;;       -90 0 0 -1)))

;; (deftest test-sample-locations-2x2
;;   (let [geo-grid (open-example-geo-grid)]
;;     (is (= (sample-locations geo-grid (make-location 78 0) :width 2 :height 2)
;;            (map #(apply make-location %)
;;                 [[78.0 0.0] [78.0 1.25]
;;                  [77.0 0.0] [77.0 1.25]])))))

;; ;; (deftest test-sample-locations-4x4
;; ;;   (let [geo-grid (open-example-geo-grid)]
;; ;;     (is (= (sample-locations geo-gridyyyyyyyyyyyyyyyyyy (make-location 78 0) 4 4)
;; ;;            (map #(apply make-location %)
;; ;;                 [[78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;; ;;                  [78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;; ;;                  [78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;; ;;                  [78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;; ;;                  ])))))

