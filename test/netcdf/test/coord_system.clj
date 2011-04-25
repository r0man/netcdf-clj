(ns netcdf.test.coord-system
  (:import ucar.unidata.geoloc.Projection)
  (:require [netcdf.geo-grid :as grid])
  (:use clojure.test
        netcdf.location
        netcdf.coord-system
        netcdf.test.helper))

(def *coord-system* (grid/coord-system (grid/open-geo-grid *dataset-uri* *variable*)))

(deftest test-make-axis
  (let [axis (make-axis -78 78 1)]
    (is (= (:min axis) -78))
    (is (= (:max axis) 78))
    (is (= (:size axis) 157))
    (is (= (:step axis) 1))))

(deftest test-latitude-axis
  (let [axis (latitude-axis *coord-system*)]
    (is (= (:min axis) -78))
    (is (= (:max axis) 78))
    (is (= (:size axis) 157))
    (is (= (:step axis) 1))))

(deftest test-longitude-axis
  (let [axis (longitude-axis *coord-system*)]
    (is (= (:min axis) 0))
    (is (= (:max axis) 358.75))
    (is (= (:size axis) 288))
    (is (= (:step axis) 1.25))))

(deftest test-location-axis
  (is (= {:latitude-axis (latitude-axis *coord-system*) :longitude-axis (longitude-axis *coord-system*)}
         (location-axis *coord-system*))))

(deftest test-location-on-grid
  (are [location expected]
    (is (= expected (location-on-grid *coord-system* location)))
    (make-location 0 0) (make-location 0 0)
    (make-location 78 0) (make-location 78 0)
    (make-location 77.5 0) (make-location 78 0)
    (make-location 77.4 0) (make-location 77 0)
    (make-location 78 0.624) (make-location 78 0)
    (make-location 78 0.625) (make-location 78 1.25)))

(deftest test-projection
  (is (isa? (class (projection *coord-system*)) Projection)))

(deftest test-x-y-index
  (is (= (x-y-index *coord-system* (make-location 0 0)) [0 78]))
  (is (= (x-y-index *coord-system* (make-location 900 900)) [144 -1])))

(deftest test-location-on-grid
  (is (nil? (location-on-grid *coord-system* (make-location 900 900))))
  (is (= (make-location 0 0)
         (location-on-grid *coord-system* (make-location 0 0)))))

(deftest test-fraction-of-latitudes
  (are [location-1 location-2 fraction]
    (is (= fraction (fraction-of-latitudes *coord-system* location-1 location-2)))
    (make-location 78 0) (make-location 78 0) 0
    (make-location 78 0) (make-location 77.5 0) 0.5
    (make-location 78 0) (make-location 77 0) 1))

(deftest test-fraction-of-longitudes
  (are [location-1 location-2 fraction]
    (is (= fraction (fraction-of-longitudes *coord-system* location-1 location-2)))
    (make-location 78 0) (make-location 78 0) 0
    (make-location 78 0) (make-location 78 0.625) 0.5
    (make-location 78 0) (make-location 78 1.25) 1))

(deftest test-max-axis
  (let [smaller {:min -76.0, :max 77.0, :size 154, :step 1.0}
        larger {:min -78.0, :max 78.0, :size 157, :step 1.0}]
    (testing "with one argument"
      (is (= larger (max-axis larger))))
    (testing "with two arguments"
      (let [axis (max-axis larger larger)]
        (is (= (:min larger) (:min axis)))
        (is (= (:max larger) (:max axis)))
        (is (= (:size larger) (:size axis)))
        (is (= (:step larger) (:step axis))))
      (let [axis (max-axis smaller larger)]
        (is (= -78.0 (:min axis)))
        (is (= 78.0 (:max axis)))
        (is (= 157 (:size axis)))
        (is (= 1.0 (:step axis)))))
    (testing "with more arguments"
      (is (= larger (max-axis larger smaller larger))))))

(deftest test-min-axis
  (let [smaller {:min -76.0, :max 77.0, :size 154, :step 1.0}
        larger {:min -78.0, :max 78.0, :size 157, :step 1.0}]
    (testing "with one argument"
      (is (= larger (min-axis larger))))
    (testing "with two arguments"
      (let [axis (min-axis larger larger)]
        (is (= (:min larger) (:min axis)))
        (is (= (:max larger) (:max axis)))
        (is (= (:size larger) (:size axis)))
        (is (= (:step larger) (:step axis))))
      (let [axis (min-axis smaller larger)]
        (is (= -76.0 (:min axis)))
        (is (= 77.0 (:max axis)))
        (is (= 154 (:size axis)))
        (is (= 1.0 (:step axis)))))
    (testing "with more arguments"
      (is (= smaller (min-axis larger smaller larger))))))

(deftest test-resolution
  (let [resolution (resolution *coord-system*)]
    (is (= 1.25 (:width resolution)))
    (is (= 1.0 (:height resolution)))))