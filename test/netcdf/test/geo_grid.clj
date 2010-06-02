(ns netcdf.test.geo-grid
  (:import ucar.unidata.geoloc.Projection)
  (:use [incanter.core :only (matrix?)]
        clojure.test netcdf.geo-grid netcdf.location netcdf.test.helper)
  (:require [netcdf.dataset :as dataset]))

(defn open-example-geo-grid []
  (open-geo-grid *dataset-uri* *variable*))

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
    (is (= (:lat-min axis) -78))
    (is (= (:lat-max axis) 78))
    (is (= (:lat-size axis) 157))
    (is (= (:lat-step axis) 1))))

(deftest test-lon-axis
  (let [axis (lon-axis (open-example-geo-grid))]
    (is (= (:lon-min axis) 0))
    (is (= (:lon-max axis) 358.75))
    (is (= (:lon-size axis) 288))
    (is (= (:lon-step axis) 1.25))))

(deftest test-lat-lon-axis
  (let [geo-grid (open-example-geo-grid)]
    (is (= (lat-lon-axis geo-grid) (merge (lat-axis geo-grid) (lon-axis geo-grid))))))

(deftest test-time-axis
  (let [geo-grid (open-example-geo-grid)]
    (is (isa? (class (time-axis geo-grid)) ucar.nc2.dataset.CoordinateAxis1DTime))))

(deftest test-projection
  (let [projection (projection (open-example-geo-grid))]
    (is (isa? (class projection) ucar.unidata.geoloc.ProjectionImpl))))

(deftest test-read-seq
  (let [geo-grid (open-example-geo-grid)
        valid-time (first (valid-times geo-grid))
        sequence (read-seq geo-grid)]
    (is (seq? sequence))
    (is (= (count sequence) 45216))
    (let [m (meta sequence)]
      (is (= (:valid-time m) valid-time))
      (is (isa? (class (:projection m)) Projection))
      (is (= (:lat-max m) 78))
      (is (= (:lat-min m) -78))
      (is (= (:lat-step m) 1))      
      (is (= (:lon-max m) 358.75))
      (is (= (:lon-min m) 0))
      (is (= (:lon-step m) 1.25)))))

(deftest test-read-matrix
  (let [geo-grid (open-example-geo-grid)
        matrix (read-matrix geo-grid)]
    (is (matrix? matrix))
    (is (= (count matrix) 157))
    (is (every? #(= % 288) (map count matrix)))    
    (let [m (meta matrix)]
      (is (= (:valid-time m) (first (valid-times geo-grid))))
      (is (isa? (class (:projection m)) Projection))
      (is (= (:lat-max m) 78))
      (is (= (:lat-min m) -78))
      (is (= (:lat-step m) 1))      
      (is (= (:lon-max m) 358.75))
      (is (= (:lon-min m) 0))
      (is (= (:lon-step m) 1.25)))))

(deftest test-time-index-with-geo-grid
  (let [geo-grid (open-example-geo-grid)]
    (is (= (time-index geo-grid (first (valid-times geo-grid))) 0))
    (is (= (time-index geo-grid (last (valid-times geo-grid))) (- (count (valid-times geo-grid)) 1)))))

(deftest test-valid-times
  (let [valid-times (valid-times (open-example-geo-grid))]
    (is (> (count valid-times) 0))
    (is (every? #(isa? (class %) java.util.Date) valid-times))))

(deftest test-location->row-column
  (let [matrix (read-matrix (open-example-geo-grid))]
    (are [latitude longitude row column]
      (is (= (location->row-column matrix (make-location latitude longitude)) [row column]))
      78 0 0 0
      78 0.1 0 0
      78 1.24 0 0
      78 1.25 0 1
      78 180 0 144
      78 -178.5 0 145
      78 -1.25 0 287
      77 0 1 0
      77.1 0 0 0
      77.9 0 0 0)))

(deftest test-sel-location
  (let [matrix (read-matrix (open-example-geo-grid))]
    (is (.isNaN (sel-location matrix (make-location 78 0))))
    (is (.isNaN (sel-location matrix (make-location 78 -1.25))))
    (is (.isNaN (sel-location matrix (make-location -77 0))))
    (is (.isNaN (sel-location matrix (make-location -77 -1.25))))
    ;; (is (= (sel-location matrix (make-location 77 0)) 1.809999942779541))
    ;; (is (= (sel-location matrix (make-location -70 0)) 1.5800000429153442))
    ))

(deftest test-sel-location!
  (let [matrix (read-matrix (open-example-geo-grid))]
    (is (.isNaN (sel-location! matrix (make-location 78 0))))
    (is (.isNaN (sel-location! matrix (make-location 78 -1.25))))
    (is (.isNaN (sel-location! matrix (make-location -77 0))))
    (is (.isNaN (sel-location! matrix (make-location -77 -1.25))))
    ;; (is (= (sel-location! matrix (make-location 77 0)) 1.809999942779541))
    ;; (is (= (sel-location! matrix (make-location -70 0)) 1.5800000429153442))
    ))

