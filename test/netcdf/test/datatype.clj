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

(deftest test-coord-system
  (let [datatype (open-example-datatype) coord-system (coord-system datatype)]
    (is (isa? (class coord-system) ucar.nc2.dt.grid.GridCoordSys))))

(deftest test-description
  (is (= (description (open-example-datatype)) "** surface sig height of wind waves and swell [m]")))

(deftest test-projection
  (let [datatype (open-example-datatype) projection (projection datatype)]
    (is (isa? (class projection) ucar.unidata.geoloc.ProjectionImpl))))

(deftest test-latitude-axis
  (let [axis (latitude-axis (open-example-datatype))]
    (is (= (:lat-min axis) -78))
    (is (= (:lat-max axis) 78))
    (is (= (:lat-size axis) 157))
    (is (= (:lat-step axis) 1))))

(deftest test-longitude-axis
  (let [axis (longitude-axis (open-example-datatype))]
    (is (= (:lon-min axis) 0))
    (is (= (:lon-max axis) 358.75))
    (is (= (:lon-size axis) 288))
    (is (= (:lon-step axis) 1.25))))

(deftest test-axis
  (let [datatype (open-example-datatype)]
    (is (= (axis datatype) (merge (latitude-axis datatype) (longitude-axis datatype))))))

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
    (is (= (select-keys datatype (keys (latitude-axis datatype))) (latitude-axis datatype)))
    (is (= (select-keys datatype (keys (longitude-axis datatype))) (longitude-axis datatype)))))

(deftest test-read-seq
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))
        sequence (read-seq datatype)]
    (is (seq? sequence))
    (is (= (count sequence) 45216))
    (let [m (meta sequence)]
      (is (= (:datatype m) datatype))
      (is (= (:valid-time m) valid-time)))))

(deftest test-read-matrix
  (let [datatype (open-example-datatype)
        sequence (read-seq datatype)
        matrix (read-matrix datatype)]
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
      (is (= (:valid-time m) (first (valid-times datatype))))
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
        sequence (read-seq datatype)
        matrix (read-matrix datatype :width 5 :height 5)]
    (is (matrix? matrix))
    (is (= (count sequence) 4))
    (is (= (count matrix) 5))
    (is (every? #(= % 5) (map count matrix)))    
    (is (= (sel matrix 0 0) (:value (nth sequence 0))))
    (is (= (sel matrix 0 1) (:value (nth sequence 1))))
    (is (= (sel matrix 1 0) (:value (nth sequence 2))))
    (is (= (sel matrix 1 1) (:value (nth sequence 3))))
    (let [m (meta matrix)]
      (is (= (:description m) (description datatype)))
      (is (= (:valid-time m) (first (valid-times datatype))))
      (is (= (:variable m) (:variable datatype)))
      (is (= (:lat-max m) 77))
      (is (= (:lat-min m) 73))
      (is (= (:lat-size m) 5))
      (is (= (:lat-step m) (:lat-step datatype)))
      (is (= (:lon-max m) 5))
      (is (= (:lon-min m) 0))
      (is (= (:lon-size m) 5))
      (is (= (:lon-step m) (:lon-step datatype))))))

(deftest test-read-matrix
  (let [datatype (open-example-datatype)
        matrix (read-matrix datatype)]
    ;; (println (sel matrix :rows (range 15) :cols (range 15)))
    (is (matrix? matrix))
    (is (= (count matrix) 157))
    (is (every? #(= % 288) (map count matrix)))    
    (let [m (meta matrix)]
      (is (= (:datatype m) datatype))
      (is (= (:valid-time m) (first (valid-times datatype)))))))

(deftest test-read-datapoint-with-datatype
  (let [datatype (open-example-datatype)
        valid-time (first (valid-times datatype))]
    (let [data (read-datapoint datatype (make-location 0 0) :valid-time valid-time)]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 0 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*)))
    (let [data (read-datapoint datatype (make-location 78 0) :valid-time valid-time)]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 78 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*))
      (is (.isNaN (:value data))))
    (let [data (read-datapoint datatype (make-location 78 0) :valid-time valid-time :nil -999)]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 78 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*))
      (is (= (:value data) -999)))))

(deftest test-read-datapoint-with-matrix
  (let [datatype (open-example-datatype)
        matrix (read-matrix datatype)
        valid-time (first (valid-times datatype))]
    (let [data (read-datapoint matrix (make-location 75 0) :valid-time valid-time)]
      (is (= (:actual-location data) (make-location 75 0)))
      (is (= (:requested-location data) (make-location 75 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*)))
    (let [data (read-datapoint matrix (make-location 78 0) :valid-time valid-time)]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 78 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*))
      (is (.isNaN (:value data))))
    (let [data (read-datapoint matrix (make-location 78 0) :valid-time valid-time :nil -999)]
      (is (location? (:actual-location data)))
      (is (= (:requested-location data) (make-location 78 0)))
      (is (= (:valid-time data) valid-time))
      (is (= (:variable data) *variable*))
      (is (= (:value data) -999)))))

(deftest test-time-index-with-datatype
  (let [datatype (open-example-datatype)]
    (is (= (time-index datatype (first (valid-times datatype))) 0))
    (is (= (time-index datatype (last (valid-times datatype))) (- (count (valid-times datatype)) 1)))))

(deftest test-time-index-with-geogrid
  (let [datatype (:service (open-example-datatype))]
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

(deftest test-datatype-subset
  (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
    (let [subset (datatype-subset datatype valid-time (make-location 0 0))]
      (is (= (:dataset-uri subset)) (:dataset-uri datatype))
      (is (= (:variable subset) (:variable datatype)))
      (is (not (= (:service subset) (:service datatype))))
      ;; (is (= (:lat-min subset) 0))
      ;; (is (= (:lat-max subset) 78))
      ;; (is (= (:lat-size subset) 3))
      (is (= (:lon-min subset) 0))
      (is (= (:lon-max subset) 1.25))
      (is (= (:lon-size subset) 2)))
    (let [subset (datatype-subset datatype valid-time (make-location 78 0) :width 2 :height 3)]
      (is (= (:dataset-uri subset)) (:dataset-uri datatype))
      (is (= (:variable subset) (:variable datatype)))
      (is (not (= (:service subset) (:service datatype))))
      (is (= (:lat-min subset) 76))
      (is (= (:lat-max subset) 78))
      (is (= (:lat-size subset) 3))
      (is (= (:lon-min subset) 0))
      (is (= (:lon-max subset) 1.25))
      (is (= (:lon-size subset) 2)))))

(deftest test-location->index
  (let [datatype (open-example-datatype)]
    (are [latitude longitude x y]
      (is (= (location->index datatype (make-location latitude longitude)) {:x x :y y}))
      0 0 0 78
      78 -180 144 156
      78 180 144 156
      78 179 143 156
      -78 180 144 0
      -78 179 143 0
      90 0 0 -1
      -90 0 0 -1)))

(deftest test-sample-locations-2x2
  (let [datatype (open-example-datatype)]
    (is (= (sample-locations datatype (make-location 78 0) 2 2)
           (map #(apply make-location %)
                [[78.0 0.0] [78.0 1.25]
                 [77.0 0.0] [77.0 1.25]])))))

;; (deftest test-sample-locations-4x4
;;   (let [datatype (open-example-datatype)]
;;     (is (= (sample-locations datatype (make-location 78 0) 4 4)
;;            (map #(apply make-location %)
;;                 [[78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;;                  [78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;;                  [78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;;                  [78.0 0.0] [78.0 1.25] [77.0 0.0] [77.0 1.25]
;;                  ])))))

