(ns netcdf.test.dataset
  (:use clojure.test netcdf.dataset netcdf.test.helper))

;; (defn make-example-dataset []
;;   (make-dataset *dataset-uri*))

;; (deftest test-make-dataset
;;   (let [dataset (make-example-dataset)]
;;     (is (= (:uri dataset) *dataset-uri*))
;;     (is (nil? (:service dataset))))
;;   (let [dataset (make-dataset *remote-uri*)]
;;     (is (= (:uri dataset) *remote-uri*))
;;     (is (nil? (:service dataset)))))

(deftest test-open-dataset
  (let [dataset (open-dataset *dataset-uri*)]
    (is (isa? (class dataset) ucar.nc2.dataset.NetcdfDataset))))

(deftest test-open-dataset-with-remote
  (let [dataset (open-dataset *remote-uri*)]
    (is (isa? (class dataset) ucar.nc2.dataset.NetcdfDataset))))

(deftest test-open-grid-dataset
  (let [dataset (open-grid-dataset *dataset-uri*)]
    (is (isa? (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-open-grid-dataset-with-remote
  (let [dataset (open-grid-dataset *remote-uri*)]
    (is (isa? (class dataset) ucar.nc2.dt.grid.GridDataset))))

(deftest test-copy-dataset
  (let [target "/tmp/.copy-test.netcdf"]
    (copy-dataset *dataset-uri* target)
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-copy-dataset-selected-variables
  (let [target "/tmp/.copy-test.netcdf"]
    (copy-dataset *dataset-uri* target ["htsgwsfc"])
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-valid-times
  (let [valid-times (valid-times (open-grid-dataset *dataset-uri*))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))
