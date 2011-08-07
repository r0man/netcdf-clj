(ns netcdf.test.dataset
  (:import ucar.nc2.dt.grid.GeoGrid)
  (:use clojure.test
        netcdf.dataset
        netcdf.test.helper
        netcdf.time))

(deftest test-open-dataset
  (with-open [dataset (open-dataset example-path)]
    (is (isa? (class dataset) ucar.nc2.dataset.NetcdfDataset))))

(deftest test-open-dataset-with-remote
  (with-open [dataset (open-dataset *remote-uri*)]
    (is (isa? (class dataset) ucar.nc2.dataset.NetcdfDataset))))

(deftest test-open-grid-dataset
  (with-open [dataset (open-grid-dataset example-path)]
    (is (isa? (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-open-grid-dataset-with-remote
  (with-open [dataset (open-grid-dataset *remote-uri*)]
    (is (isa? (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-copy-dataset
  (let [target "/tmp/copy-dataset-path-as-string"]
    (copy-dataset example-path target)
    (is (= (.exists (java.io.File. target)) true)))
  (let [target (java.net.URI. "file:/tmp/copy-dataset-uri")]
    (copy-dataset example-path target)
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-copy-dataset-with-some-variables
  (let [target "/tmp/.copy-test.netcdf"]
    (copy-dataset example-path target ["htsgwsfc"])
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-datatype-names
  (with-open-grid-dataset [dataset example-path]
    (is (= (datatype-names dataset) ["htsgwsfc"]))))

(deftest test-find-geo-grid
  (with-open-grid-dataset [dataset example-path]
    (let [geo-grid (last (geo-grids dataset)) name (.getName geo-grid)]
      (is (= geo-grid (find-geo-grid dataset name))))))

(deftest test-find-grid-datatype
  (with-open-grid-dataset [dataset example-path]
    (let [datatype (find-grid-datatype dataset "htsgwsfc")]
      (is (isa? (class datatype) ucar.nc2.dt.GridDatatype))
      (is (= "htsgwsfc" (.getName datatype))))))

(deftest test-geo-grids
  (with-open-grid-dataset [dataset example-path]
    (let [grids (geo-grids dataset)]
      (is (not (empty? grids)))
      (is (every? #(isa? (class %) GeoGrid) grids)))))

(deftest test-valid-times
  (with-open-grid-dataset [dataset example-path]
    (let [valid-times (valid-times dataset)]
      (is (not (empty? valid-times)))
      (is (every? date-time? valid-times)))))

(deftest test-with-open-dataset
  (with-open-dataset [dataset example-path]
    (is (isa? (class dataset) ucar.nc2.dataset.NetcdfDataset))))

(deftest test-with-open-grid-dataset
  (with-open-grid-dataset [dataset example-path]
    (is (isa? (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-write-csv
  (with-open-grid-dataset [dataset example-path]
    (write-csv dataset "/tmp/netcdf.csv")))