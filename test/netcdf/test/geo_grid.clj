(ns netcdf.test.geo-grid
  (:import ucar.unidata.geoloc.Projection)
  (:use [clj-time.core :only (date-time)]
        [clojure.contrib.duck-streams :only (read-lines)]
        [incanter.core :only (matrix?)]
        clojure.test
        netcdf.coord-system
        netcdf.geo-grid
        netcdf.location
        netcdf.test.helper
        netcdf.time)
  (:require [netcdf.dataset :as dataset]))

(refer-private 'netcdf.geo-grid)

(defn open-example-geo-grid []
  (open-geo-grid *dataset-uri* *variable*))

(deftest test-open-geo-grid
  (let [geo-grid (open-example-geo-grid)]
    (is (= (class geo-grid) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-bounding-box
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (let [bounds (bounding-box grid)]
      (is (= (class bounds) ucar.unidata.geoloc.LatLonRect)))))

(deftest test-coord-system
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (let [coord-system (coord-system grid)]
      (is (isa? (class coord-system) ucar.nc2.dt.grid.GridCoordSys)))))

(deftest test-description
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (is (= (description grid) "** surface none significant height of combined wind waves and swell [m]"))))

(deftest test-dimensions
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (let [dimensions (dimensions grid)]
      (is (seq? dimensions))
      (is (every? #(isa? (class %) ucar.nc2.Dimension) dimensions)))))

(deftest test-format-record
  (are [record line]
    (is (= line (format-record record)))
    {:location (make-location 1 2)
     :variable "htsgwsfc"
     :valid-time (date-time 2010 12 18)
     :value 1}
    "2010-12-18 00:00:00,htsgwsfc,1.0,2.0,1"
    {:location (make-location 1 2)
     :variable "htsgwsfc"
     :valid-time (date-time 2010 12 18)
     :value Double/NaN}
    "2010-12-18 00:00:00,htsgwsfc,1.0,2.0,NaN"))

(deftest test-filter-records
  (is (= [{:location (make-location 1 2) :variable "htsgwsfc" :valid-time (date-time 2010 12 18) :value 1}]
           (filter-records
            [{:location (make-location 1 2) :variable "htsgwsfc" :valid-time (date-time 2010 12 18) :value 1}
             {:location (make-location 1 2) :variable "htsgwsfc" :valid-time (date-time 2010 12 18) :value Double/NaN}]))))

(deftest test-meta-data
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (let [meta (meta-data grid)]
      (is (= (:name meta) (.getName grid)))
      (is (= (:description meta) (.getDescription grid)))
      (is (= (:latitude-axis meta) (latitude-axis (coord-system grid))))
      (is (= (:longitude-axis meta) (longitude-axis (coord-system grid)))))))

(deftest test-time-axis
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (is (isa? (class (time-axis grid)) ucar.nc2.dataset.CoordinateAxis1DTime))))

(deftest test-vertical-axis
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (is (nil? (class (vertical-axis grid))))))

(deftest test-z-index
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (is (= (z-index grid 0) 0))))

(deftest test-time-index-with-geo-grid
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (is (= (time-index grid (first (valid-times grid))) 0))
    (is (= (time-index grid (last (valid-times grid)))
           (- (count (valid-times grid)) 1)))))

(deftest test-valid-times
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (let [valid-times (valid-times grid)]
      (is (> (count valid-times) 0))
      (is (every? date-time? valid-times)))))

(deftest test-read-location
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (let [value (read-location grid {:latitude 76 :longitude 0})]
      (is (isa? (class value) Double)))))

(deftest test-read-locations
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (= [(read-location grid (make-location 77 0))]
         (read-locations grid [(make-location 77 0)]))))

(deftest test-read-x-y
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (is (isa? (class (read-x-y grid 0 0)) Double))))

(deftest test-interpolate-location
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (= (read-location grid (make-location 77 0))
       (interpolate-location grid (make-location 77 0)))
    (nil? (interpolate-location grid (make-location 900 900)))))

(deftest test-interpolate-locations
  (with-open-geo-grid [grid *dataset-uri* *variable*]
    (= [(interpolate-location grid (make-location 77 0))]
         (interpolate-locations grid [(make-location 77 0)]))))

(deftest test-with-open-geo-grid
  (with-open-geo-grid [geo-grid *dataset-uri* *variable*]
    (is (isa? (class geo-grid) ucar.nc2.dt.grid.GeoGrid))))

;; TODO: fix slow tests

;; (deftest test-read-seq
;;   (let [geo-grid (open-example-geo-grid)
;;         valid-time (first (valid-times geo-grid))
;;         sequence (read-seq geo-grid)]
;;     (is (seq? sequence))
;;     (let [meta (meta sequence)]
;;       (is (= (dissoc meta :valid-time) (meta-data geo-grid)))
;;       (is (= (:valid-time meta) valid-time)))
;;     (let [record (first sequence)]
;;       (is (= (.getName geo-grid) (:variable record)))
;;       (is (location? (:location record)))
;;       (is (= (make-location -78 0) (:location record)))
;;       (is (= valid-time (:valid-time record)))
;;       (is (isa? (class (:value record)) java.lang.Double)))
;;     (let [record (second sequence)]
;;       (is (= (.getName geo-grid) (:variable record)))
;;       (is (location? (:location record)))
;;       (is (= (make-location -78 1.25) (:location record)))
;;       (is (= valid-time (:valid-time record)))
;;       (is (isa? (class (:value record)) java.lang.Double)))
;;     (is (= (count sequence) 45216))))

;; (deftest test-read-matrix
;;   (let [geo-grid (open-example-geo-grid)
;;         matrix (read-matrix geo-grid)]
;;     (is (matrix? matrix))
;;     (is (= (count matrix) 157))
;;     (is (every? #(= % 288) (map count matrix)))
;;     (let [meta (meta matrix)]
;;       (is (= (dissoc meta :valid-time) (meta-data geo-grid)))
;;       (is (= (:valid-time meta) (first (valid-times geo-grid)))))))

;; (deftest test-write-csv
;;   (let [grid (open-example-geo-grid) filename "/tmp/netcdf.csv"]
;;     (testing "all records"
;;       (write-csv grid filename)
;;       (is (= 45216 (count (read-lines filename)))))
;;     (testing "filtered records"
;;       (write-csv grid filename :remove #(Double/isNaN (:value %)))
;;       (is (> (count (read-lines filename)) 2000)))))
