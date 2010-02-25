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
    (is (= (central-sample-location (make-location lat lon) lat-step lon-step)
           (make-location expect-lat expect-lon)))
    0 0 1 1 0 0
    0 0 1 1 0 0
    77.0 0 1 1.25 77 0
    77.1 0 1 1.25 78 0
    77.9 0 1 1.25 78 0
    77 0.0 1 1.25 77 0
    77 0.1 1 1.25 77 0
    77 0.9 1 1.25 77 0))

(deftest test-read-sample-2x2
  (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
    (is (= (read-sample-2x2 datatype valid-time (make-location 77 0))
           (read-matrix datatype valid-time (make-location 77 0) :width 2 :height 2)))))

(deftest test-read-sample-4x4
  (let [datatype (open-example-datatype) valid-time (first (valid-times datatype))]
    (is (= (read-sample-4x4 datatype valid-time (make-location 77 1.25))
           (read-matrix datatype valid-time (make-location 78 0) :width 4 :height 4)))))

