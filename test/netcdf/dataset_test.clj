(ns netcdf.dataset-test
  (:import ucar.nc2.dt.grid.GeoGrid)
  (:require [netcdf.dods :as dods]
            [netcdf.model :refer [nww3]]
            [clj-time.core :refer [date-time]])
  (:use clojure.test
        netcdf.dataset
        netcdf.test
        netcdf.time))

(defonce example-remote-url
  (:dods (dods/datasource nww3 example-reference-time)))

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
  (with-grid-dataset [dataset example-path]
    (is (= (datatype-names dataset) ["htsgwsfc"]))))

(deftest test-find-geo-grid
  (with-grid-dataset [dataset example-path]
    (let [geo-grid (last (geo-grids dataset)) name (.getName geo-grid)]
      (is (= geo-grid (find-geo-grid dataset name))))))

(deftest test-find-grid-datatype
  (with-grid-dataset [dataset example-path]
    (let [datatype (find-grid-datatype dataset "htsgwsfc")]
      (is (instance? ucar.nc2.dt.GridDatatype datatype))
      (is (= "htsgwsfc" (.getName datatype))))))

(deftest test-geo-grids
  (with-grid-dataset [dataset example-path]
    (let [grids (geo-grids dataset)]
      (is (not (empty? grids)))
      (is (every? #(instance? GeoGrid %) grids)))))

(deftest test-valid-times
  (with-grid-dataset [dataset example-path]
    (let [valid-times (valid-times dataset)]
      (is (not (empty? valid-times)))
      (is (every? date-time? valid-times)))))

(deftest test-with-dataset
  (with-dataset [dataset example-path]
    (is (instance? ucar.nc2.dataset.NetcdfDataset dataset))))

(deftest test-with-grid-dataset
  (with-grid-dataset [dataset example-path]
    (is (instance? ucar.nc2.dt.grid.GridDataset dataset))))

(deftest test-write-dataset
  (with-grid-dataset [dataset example-path]
    (write-dataset dataset "/tmp/netcdf.csv")))

(deftest test-write-geotiff
  (with-grid-dataset [dataset example-path]
    (let [filename "/tmp/test-write-geotiff.tif"
          time (first (valid-times dataset))]
      (is (= filename (write-geotiff dataset example-variable time filename false)))
      (is (.exists (java.io.File. filename))))))

(deftest test-geotiff-filename
  (is (= "htsgwsfc/2013/01/01/00.tif"
         (geotiff-filename "htsgwsfc" (date-time 2013 1 1)))))

(deftest test-write-geotiffs
  (with-grid-dataset [dataset example-path]
    (let [directory "/tmp/test-write-geotiffs"
          filenames (write-geotiffs dataset [example-variable] directory)]
      (is (= 61 (count filenames))))))
