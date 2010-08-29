(ns netcdf.test.geo-grid
  (:import ucar.unidata.geoloc.Projection)
  (:use [incanter.core :only (matrix?)]
        clojure.test netcdf.geo-grid netcdf.location netcdf.test.helper)
  (:require [netcdf.dataset :as dataset]))

(refer-private 'netcdf.geo-grid)

(defn open-example-geo-grid []
  (open-geo-grid *dataset-uri* *variable*))

(deftest test-normalize-lon-axis
  (let [axis (normalize-lon-axis {:min 0.0, :max 358.75, :size 288, :step 1.25})]
    (is (= (:min axis) -179.375))
    (is (= (:max axis) 179.375))
    (is (= (:size axis) 288))
    (is (= (:step axis) 1.25)))
  (let [axis {:min 170, :max 180, :size 10, :step 1}]
    (is (= (normalize-lon-axis axis) axis))))

(deftest test-open-geo-grid
  (let [geo-grid (open-example-geo-grid)]
    (is (= (class geo-grid) ucar.nc2.dt.grid.GeoGrid))))

(deftest test-bounding-box
  (let [bounds (bounding-box (open-example-geo-grid))]
    (is (= (class bounds) ucar.unidata.geoloc.LatLonRect))))

(deftest test-coord-system
  (let [coord-system (coord-system (open-example-geo-grid))]
    (is (isa? (class coord-system) ucar.nc2.dt.grid.GridCoordSys))))

(deftest test-description
  (is (= (description (open-example-geo-grid)) "** surface sig height of wind waves and swell [m]")))

(deftest test-lat-axis
  (let [axis (lat-axis (open-example-geo-grid))]
    (is (= (:min axis) -78))
    (is (= (:max axis) 78))
    (is (= (:size axis) 157))
    (is (= (:step axis) 1))))

(deftest test-lon-axis
  (let [axis (lon-axis (open-example-geo-grid))]
    ;; (is (= (:min axis) 0)) ; not normalized
    ;; (is (= (:max axis) 358.75))
    (is (= (:min axis) -179.375))
    (is (= (:max axis) 179.375))
    (is (= (:size axis) 288))
    (is (= (:step axis) 1.25))))

(deftest test-meta-data
  (let [grid (open-example-geo-grid)
        meta (meta-data grid)]
    (is (= (:name meta) (.getName grid)))
    (is (= (:description meta) (.getDescription grid)))
    (is (= (:lat-axis meta) (lat-axis grid)))
    (is (= (:lon-axis meta) (lon-axis grid)))))

(deftest test-lat-lon-axis
  (let [geo-grid (open-example-geo-grid)]
    (is (= (lat-lon-axis geo-grid) {:lat-axis (lat-axis geo-grid) :lon-axis (lon-axis geo-grid)}))))

(deftest test-time-axis
  (let [geo-grid (open-example-geo-grid)]
    (is (isa? (class (time-axis geo-grid)) ucar.nc2.dataset.CoordinateAxis1DTime))))

(deftest test-vertical-axis
  (let [geo-grid (open-example-geo-grid)]
    (is (nil? (class (vertical-axis geo-grid)))))) ;; TODO: Use dataset with a vertical axis

(deftest test-z-index
  (let [grid (open-example-geo-grid)]
    (is (= (z-index grid 0) 0))))

(deftest test-projection
  (let [projection (projection (open-example-geo-grid))]
    (is (isa? (class projection) ucar.unidata.geoloc.ProjectionImpl))))

(deftest test-read-seq
  (let [geo-grid (open-example-geo-grid)
        valid-time (first (valid-times geo-grid))
        sequence (read-seq geo-grid)]
    (is (seq? sequence))
    (is (= (count sequence) 45216))
    (let [meta (meta sequence)]
      (is (= (dissoc meta :valid-time) (meta-data geo-grid)))
      (is (= (:valid-time meta) valid-time)))))

(deftest test-read-matrix
  (let [geo-grid (open-example-geo-grid)
        matrix (read-matrix geo-grid)]
    (is (matrix? matrix))
    (is (= (count matrix) 157))
    (is (every? #(= % 288) (map count matrix)))    
    (let [meta (meta matrix)]
      (is (= (dissoc meta :valid-time) (meta-data geo-grid)))
      (is (= (:valid-time meta) (first (valid-times geo-grid)))))))

(deftest test-time-index-with-geo-grid
  (let [geo-grid (open-example-geo-grid)]
    (is (= (time-index geo-grid (first (valid-times geo-grid))) 0))
    (is (= (time-index geo-grid (last (valid-times geo-grid)))
           (- (count (valid-times geo-grid)) 1)))))

(deftest test-valid-times
  (let [valid-times (valid-times (open-example-geo-grid))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) org.joda.time.DateTime) valid-times))))

(deftest test-read-location
  (let [grid (open-example-geo-grid)
        value (read-location grid {:latitude 76 :longitude 0})]
    (is (isa? (class value) Double))))

;; (deftest test-location->row-column
;;   (let [matrix (read-matrix (open-example-geo-grid))]
;;     (are [latitude longitude row column]
;;       (is (= (location->row-column matrix (make-location latitude longitude)) [row column]))
;;       78 0 0 0
;;       78 0.1 0 0
;;       78 1.24 0 0
;;       78 1.25 0 1
;;       78 180 0 144
;;       78 -178.5 0 145
;;       78 -1.25 0 287
;;       77 0 1 0
;;       77.1 0 0 0
;;       77.9 0 0 0)))

;; (deftest test-sel-location
;;   (let [matrix (read-matrix (open-example-geo-grid))]
;;     (is (.isNaN (sel-location matrix (make-location 78 0))))
;;     (is (.isNaN (sel-location matrix (make-location 78 -1.25))))
;;     (is (.isNaN (sel-location matrix (make-location -77 0))))
;;     (is (.isNaN (sel-location matrix (make-location -77 -1.25))))
;;     ;; (is (= (sel-location matrix (make-location 77 0)) 1.809999942779541))
;;     ;; (is (= (sel-location matrix (make-location -70 0)) 1.5800000429153442))
;;     ))

;; (deftest test-sel-location!
;;   (let [matrix (read-matrix (open-example-geo-grid))]
;;     (is (.isNaN (sel-location! matrix (make-location 78 0))))
;;     (is (.isNaN (sel-location! matrix (make-location 78 -1.25))))
;;     (is (.isNaN (sel-location! matrix (make-location -77 0))))
;;     (is (.isNaN (sel-location! matrix (make-location -77 -1.25))))
;;     ;; (is (= (sel-location! matrix (make-location 77 0)) 1.809999942779541))
;;     ;; (is (= (sel-location! matrix (make-location -70 0)) 1.5800000429153442))
;;     ))

