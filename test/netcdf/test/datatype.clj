(ns netcdf.test.datatype
  (:use clojure.test netcdf.datatype netcdf.location))

(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
(def *variable* "htsgwsfc")

(def *datatype* (open-datatype *dataset-uri* *variable*))
(def *valid-time* (first (valid-times *datatype*)))

(deftest test-open-datatype
  (let [datatype (open-datatype *dataset-uri* *variable*)]
    (is (= (class datatype) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-read-dataset
  (let [location (make-location 0 0)
        record (read-datatype *datatype* *valid-time* location)]
    (is (= (:actual-location record) (make-location 0 0 0)))
    (is (= (:distance record) 0))
    (is (= (:requested-location record) location))
    (is (= (:valid-time record) *valid-time*))
    (is (= (:variable record) *variable*))))

(deftest test-valid-times
  (let [valid-times (valid-times *datatype*)]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))
