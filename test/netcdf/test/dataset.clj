(ns netcdf.test.dataset
  (:import ucar.nc2.dt.grid.GeoGrid)
  (:require [netcdf.dods :as dods])
  (:use [netcdf.model :only (nww3)]
        clojure.test
        netcdf.dataset
        netcdf.test.helper
        netcdf.time))

(def example-remote-url
  (:dods (first (dods/datasets-by-url-and-reference-time (:dods nww3) example-reference-time))))

(deftest test-open-dataset
  (with-open [dataset (open-dataset example-path)]
    (is (instance? ucar.nc2.dataset.NetcdfDataset dataset))))

(deftest test-open-dataset-with-remote
  (with-open [dataset (open-dataset example-remote-url)]
    (is (instance? ucar.nc2.dataset.NetcdfDataset dataset))))

(deftest test-open-grid-dataset
  (with-open [dataset (open-grid-dataset example-path)]
    (is (instance? ucar.nc2.dt.grid.GridDataset dataset))))

(deftest test-open-grid-dataset-with-remote
  (with-open [dataset (open-grid-dataset example-remote-url)]
    (is (instance? ucar.nc2.dt.grid.GridDataset dataset))))

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
      (is (instance? ucar.nc2.dt.GridDatatype datatype))
      (is (= "htsgwsfc" (.getName datatype))))))

(deftest test-geo-grids
  (with-open-grid-dataset [dataset example-path]
    (let [grids (geo-grids dataset)]
      (is (not (empty? grids)))
      (is (every? #(instance? GeoGrid %) grids)))))

(deftest test-valid-times
  (with-open-grid-dataset [dataset example-path]
    (let [valid-times (valid-times dataset)]
      (is (not (empty? valid-times)))
      (is (every? date-time? valid-times)))))

(deftest test-with-open-dataset
  (with-open-dataset [dataset example-path]
    (is (instance? ucar.nc2.dataset.NetcdfDataset dataset))))

(deftest test-with-open-grid-dataset
  (with-open-grid-dataset [dataset example-path]
    (is (instance? ucar.nc2.dt.grid.GridDataset dataset))))

(deftest test-write-dataset
  (with-open-grid-dataset [dataset example-path]
    (write-dataset dataset "/tmp/netcdf.csv")))

(deftest test-write-geotiff
  (with-open-grid-dataset [dataset example-path]
    (let [filename "/tmp/test-write-geotiff.tif"
          time (first (valid-times dataset))]
      (is (= filename (write-geotiff dataset example-variable time filename false)))
      (is (.exists (java.io.File. filename))))))
