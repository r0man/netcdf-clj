(ns netcdf.test.dataset
  (:use clojure.test netcdf.dataset))

(def *dataset-uri* "/home/roman/.weather/20100215/akw.06.nc")
;;; TODO: Compute url with current date
(def *remote-uri* "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw/akw20100326/akw_00z")
(def *variable* "htsgwsfc")

(deftest test-open-dataset
  (let [dataset (open-dataset *dataset-uri*)]
    (is (= (class dataset) ucar.nc2.dataset.NetcdfDataset))
    (.close dataset)))

(deftest test-open-dataset-with-remote
  (let [dataset (open-dataset *remote-uri*)]
    (is (= (class dataset) ucar.nc2.dataset.NetcdfDataset))
    (.close dataset)))

(deftest test-open-grid-dataset
  (let [dataset (open-grid-dataset *dataset-uri*)]
    (is (= (class dataset) ucar.nc2.dt.grid.GridDataset))
    (.close dataset)))

(deftest test-open-grid-dataset-with-remote
  (let [dataset (open-grid-dataset *remote-uri*)]
    (is (= (class dataset) ucar.nc2.dt.grid.GridDataset))
    (.close dataset)))

(deftest test-copy-dataset
  (let [target "/tmp/.copy-test-all.netcdf"]
    (copy-dataset *dataset-uri* target)
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-copy-dataset-selected-variables
  (let [target "/tmp/.copy-test-selected.netcdf"]
    (copy-dataset *dataset-uri* target ["htsgwsfc"])
    (is (= (.exists (java.io.File. target)) true))))

;; (deftest test-copy-dataset-from-remote
;;   (let [target "/tmp/.copy-test.netcdf"]
;;     (copy-dataset *remote-uri* target)
;;     (is (= (.exists (java.io.File. target)) true))))

(deftest test-datatypes
  (with-open [dataset (open-grid-dataset *dataset-uri*)]
    (let [datatypes (datatypes dataset)]
      (is (> (count datatypes) 0))
      (is (every? #(isa? (class %) ucar.nc2.dt.grid.GeoGrid) datatypes)))))

(deftest test-valid-times
  (with-open [dataset (open-grid-dataset *dataset-uri*)]
    (let [valid-times (valid-times dataset)]
      (is (> (count valid-times) 0))
      (is (every? #(isa? (class %) java.util.Date) valid-times)))))
