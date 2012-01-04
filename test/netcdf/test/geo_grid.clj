(ns netcdf.test.geo-grid
  (:import ucar.unidata.geoloc.Projection)
  (:use [clj-time.core :only (date-time)]
        [clojure.java.io :only (reader)]
        [clojure.string :only (split)]
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
  (open-geo-grid example-path example-variable))

(deftest test-open-geo-grid
  (let [geo-grid (open-example-geo-grid)]
    (is (= (class geo-grid) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-bounding-box
  (with-open-geo-grid [grid example-path example-variable]
    (let [bounds (bounding-box grid)]
      (is (= (class bounds) ucar.unidata.geoloc.LatLonRect)))))

(deftest test-coord-system
  (with-open-geo-grid [grid example-path example-variable]
    (let [coord-system (coord-system grid)]
      (is (instance? ucar.nc2.dt.grid.GridCoordSys coord-system)))))

(deftest test-description
  (with-open-geo-grid [grid example-path example-variable]
    (is (= (description grid) "** surface none significant height of combined wind waves and swell [m]"))))

(deftest test-dimensions
  (with-open-geo-grid [grid example-path example-variable]
    (let [dimensions (dimensions grid)]
      (is (seq? dimensions))
      (is (every? #(instance? ucar.nc2.Dimension %) dimensions)))))

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
  (with-open-geo-grid [grid example-path example-variable]
    (let [meta (meta-data grid)]
      (is (= (:name meta) (.getName grid)))
      (is (= (:description meta) (.getDescription grid)))
      (is (= (:latitude-axis meta) (latitude-axis (coord-system grid))))
      (is (= (:longitude-axis meta) (longitude-axis (coord-system grid))))
      (let [resolution (:resolution meta)]
        (is (= 1.25 (:width resolution)))
        (is (= 1.0 (:height resolution)))))))

(deftest test-time-axis
  (with-open-geo-grid [grid example-path example-variable]
    (is (instance? ucar.nc2.dataset.CoordinateAxis1DTime (time-axis grid)))))

(deftest test-vertical-axis
  (with-open-geo-grid [grid example-path example-variable]
    (is (nil? (class (vertical-axis grid))))))

(deftest test-z-index
  (with-open-geo-grid [grid example-path example-variable]
    (is (= (z-index grid 0) 0))))

(deftest test-time-index-with-geo-grid
  (with-open-geo-grid [grid example-path example-variable]
    (is (= (time-index grid (first (valid-times grid))) 0))
    (is (= (time-index grid (last (valid-times grid)))
           (- (count (valid-times grid)) 1)))))

(deftest test-valid-times
  (with-open-geo-grid [grid example-path example-variable]
    (let [valid-times (valid-times grid)]
      (is (> (count valid-times) 0))
      (is (every? date-time? valid-times)))))

(deftest test-read-location
  (with-open-geo-grid [grid example-path example-variable]
    (let [value (read-location grid {:latitude 76 :longitude 0})]
      (is (instance? Double value)))))

(deftest test-read-locations
  (with-open-geo-grid [grid example-path example-variable]
    (= [(read-location grid (make-location 77 0))]
       (read-locations grid [(make-location 77 0)]))))

(deftest test-read-index
  (with-open-geo-grid [grid example-path example-variable]
    (is (instance? Double (read-index grid 0 0)))))

(deftest test-interpolate-location
  (with-open-geo-grid [grid example-path example-variable]
    (= (read-location grid (make-location 77 0))
       (interpolate-location grid (make-location 77 0)))
    (nil? (interpolate-location grid (make-location 900 900)))))

(deftest test-interpolate-locations
  (with-open-geo-grid [grid example-path example-variable]
    (= [(interpolate-location grid (make-location 77 0))]
       (interpolate-locations grid [(make-location 77 0)]))))

(deftest test-with-open-geo-grid
  (with-open-geo-grid [geo-grid example-path example-variable]
    (is (instance? ucar.nc2.dt.grid.GeoGrid geo-grid))))

(deftest test-read-seq
  (let [geo-grid (open-example-geo-grid)
        valid-time (first (valid-times geo-grid))
        sequence (read-seq geo-grid)]
    (is (seq? sequence))
    (let [meta (meta sequence)]
      (is (= (dissoc meta :valid-time) (meta-data geo-grid)))
      (is (= (:valid-time meta) valid-time)))
    (let [record (first sequence)]
      (is (= (.getName geo-grid) (:variable record)))
      (is (location? (:location record)))
      (is (= (make-location -78 0) (:location record)))
      (is (= valid-time (:valid-time record)))
      (is (instance? java.lang.Double (:value record))))
    (let [record (second sequence)]
      (is (= (.getName geo-grid) (:variable record)))
      (is (location? (:location record)))
      (is (= (make-location -78 1.25) (:location record)))
      (is (= valid-time (:valid-time record)))
      (is (instance? java.lang.Double (:value record))))
    (is (= (count sequence) 45216))))

(deftest test-read-matrix
  (let [geo-grid (open-example-geo-grid)
        matrix (read-matrix geo-grid)]
    (is (matrix? matrix))
    (is (= (count matrix) 157))
    (is (every? #(= % 288) (map count matrix)))
    (let [meta (meta matrix)]
      (is (= (dissoc meta :valid-time) (meta-data geo-grid)))
      (is (= (:valid-time meta) (first (valid-times geo-grid)))))))

(deftest test-to-csv
  (is (= "htsgwsfc\t1292630400000\t75.0\t-124.5\t0.5399999618530273"
         (to-csv
          {:variable "htsgwsfc"
           :location {:latitude 75.0 :longitude -124.5}
           :value 0.5399999618530273
           :valid-time (date-time 2010 12 18)})))
  (is (= "htsgwsfc,1292630400000,75.0,-124.5,0.5399999618530273"
         (to-csv
          {:variable "htsgwsfc"
           :location {:latitude 75.0 :longitude -124.5}
           :value 0.5399999618530273
           :valid-time (date-time 2010 12 18)}
          ","))))

(deftest test-dump-grid
  ;; "TODO: SLOW"
  (with-open-geo-grid [grid example-path example-variable]
    (let [lines (split (with-out-str (dump-grid grid)) #"\n")]
      (is (< 0 (count lines))))))

(deftest test-write-grid
  ;; "TODO: SLOW"
  (with-open-geo-grid [grid example-path example-variable]
    (let [filename "/tmp/netcdf.csv"]
      (write-grid grid filename)
      (is (< 0 (count (line-seq (reader filename))))))))
