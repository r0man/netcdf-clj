(ns netcdf.test.datatype
  (:use clojure.test netcdf.dataset netcdf.datatype netcdf.location))

(def *dataset* (open-grid-dataset "/home/roman/.weather/20100215/nww3.06.nc"))
(def *valid-time* (first (valid-times *dataset*)))
(def *variable* "htsgwsfc")

(def *datatype* (datatype *dataset* *variable*))

(deftest test-read-dataset
  (let [location (make-location 0 0)
        record (read-dataset *datatype* *valid-time* location)]
    (is (= (:actual-location record) (make-location 0 0 0)))
    (is (= (:distance record) 0))
    (is (= (:requested-location record) location))
    (is (= (:valid-time record) *valid-time*))
    (is (= (:variable record) *variable*))))

