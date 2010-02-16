(ns netcdf.test.dataset
  (:use clojure.test netcdf.dataset))

(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
(def *variable* "htsgwsfc")

(deftest test-open-dataset
  (let [dataset (open-dataset *dataset-uri*)]
    (is (= (class dataset) ucar.nc2.NetcdfFile))))

(deftest test-open-grid-dataset
  (let [dataset (open-grid-dataset *dataset-uri*)]
    (is (= (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-close-datatset
  (let [dataset (open-dataset *dataset-uri*)]
    (close-dataset dataset))
  (let [dataset (open-grid-dataset *dataset-uri*)]
    (close-dataset dataset)))

(deftest test-copy-dataset
  (let [target "/tmp/.copy-test.netcdf"]
    (copy-dataset *dataset-uri* target ["htsgwsfc"])
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-datatypes
  (let [datatypes (datatypes (open-grid-dataset *dataset-uri*))]
    (is (every? #(isa? (class %) ucar.nc2.dt.grid.GeoGrid) datatypes))))

(deftest test-datatype
  (let [datatype (datatype (open-grid-dataset *dataset-uri*) *variable*)]
    (is (isa? (class datatype) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-valid-times
  (let [valid-times (valid-times (open-grid-dataset *dataset-uri*))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))
