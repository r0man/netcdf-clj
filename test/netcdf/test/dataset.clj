(ns netcdf.test.dataset
  (:import ucar.nc2.dt.grid.GeoGrid)
  (:use clojure.test netcdf.dataset netcdf.test.helper))

(deftest test-open-dataset
  (with-open [dataset (open-dataset *dataset-uri*)]
    (is (isa? (class dataset) ucar.nc2.dataset.NetcdfDataset))))

(deftest test-open-dataset-with-remote
  (with-open [dataset (open-dataset *remote-uri*)]
    (is (isa? (class dataset) ucar.nc2.dataset.NetcdfDataset))))

(deftest test-open-grid-dataset
  (with-open [dataset (open-grid-dataset *dataset-uri*)]
    (is (isa? (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-open-grid-dataset-with-remote
  (with-open [dataset (open-grid-dataset *remote-uri*)]
    (is (isa? (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-copy-dataset
  (let [target "/tmp/.copy-test.netcdf"]
    (copy-dataset *dataset-uri* target)
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-copy-dataset-with-some-variables
  (let [target "/tmp/.copy-test.netcdf"]
    (copy-dataset *dataset-uri* target ["htsgwsfc"])
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-datatype-names
  (with-open [dataset (open-grid-dataset *dataset-uri*)]
    (is (= (datatype-names dataset) ["htsgwsfc"]))))

(deftest test-find-geo-grid
  (with-open [dataset (open-grid-dataset *dataset-uri*)]
    (let [geo-grid (last (geo-grids dataset)) name (.getName geo-grid)]
      (is (= geo-grid (find-geo-grid dataset name))))))

(deftest test-geo-grids
  (with-open [dataset (open-grid-dataset *dataset-uri*)]
    (let [grids (geo-grids dataset)]
      (is (not (empty? grids)))
      (is (every? #(isa? (class %) GeoGrid) grids)))))

(deftest test-valid-times
  (with-open [dataset (open-grid-dataset *dataset-uri*)]
    (let [valid-times (valid-times dataset)]
      (is (not (empty? valid-times)))
      (is (every? #(isa? (class %) java.util.Date) valid-times)))))
