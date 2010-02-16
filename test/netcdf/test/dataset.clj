(ns netcdf.test.dataset
  (:use clojure.test netcdf.dataset))

(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
(def *variable* "htsgwsfc")

(defn make-example-dataset []
  (make-dataset *dataset-uri*))

(deftest test-make-dataset
  (let [dataset (make-example-dataset)]
    (is (= (:uri dataset) *dataset-uri*))
    (is (nil? (:service dataset)))))

(deftest test-open-dataset
  (let [dataset (open-dataset (make-example-dataset))]
    (is (= (:uri dataset) *dataset-uri*))
    (is (= (class (:service dataset)) ucar.nc2.NetcdfFile))))

(deftest test-dataset-open?
  (let [dataset (make-example-dataset)]
    (is (not (dataset-open? dataset-open?)))
    (is (dataset-open? (open-dataset dataset)))))

(deftest test-open-grid-dataset
  (let [dataset (open-grid-dataset (make-example-dataset))]
    (is (= (:uri dataset) *dataset-uri*))
    (is (= (class (:service dataset)) ucar.nc2.dt.grid.GridDataset))))

(deftest test-close-datatset
  (let [dataset (open-dataset (make-example-dataset))]
    (close-dataset dataset))
  (let [dataset (open-grid-dataset (make-example-dataset))]
    (close-dataset dataset)))

;; (deftest test-copy-dataset
;;   (let [target "/tmp/.copy-test.netcdf"]
;;     (copy-dataset *dataset-uri* target)
;;     (is (= (.exists (java.io.File. target)) true))))

(deftest test-copy-dataset-with-vars
  (let [target "/tmp/.copy-test.netcdf"]
    (copy-dataset *dataset-uri* target ["htsgwsfc"])
    (is (= (.exists (java.io.File. target)) true))))

(deftest test-datatype
  (let [datatype (datatype (open-grid-dataset (make-example-dataset)) *variable*)]
    (is (= (:dataset-uri datatype) *dataset-uri*))
    (is (= (:variable datatype) *variable*))
    (is (isa? (class (:service datatype)) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-datatypes
  (let [datatypes (datatypes (open-grid-dataset (make-example-dataset)))]
    (is (> (count datatypes) 0))
    (is (every? #(isa? (class (:service %)) ucar.nc2.dt.grid.GeoGrid) datatypes))))

(deftest test-valid-times
  (let [valid-times (valid-times (open-grid-dataset (make-example-dataset)))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))
