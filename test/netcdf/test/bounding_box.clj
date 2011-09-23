(ns netcdf.test.bounding-box
  (:use clojure.test
        netcdf.bounding-box
        netcdf.location))

(deftest test-make-bounding
  (let [bounds (make-bounding-box "-90,-180" "90,180")]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) -180.0))
    (is (= (.getLonMax bounds) 180.0)))
  (let [bounds (make-bounding-box "90,180" "-90,-180")]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) -180.0))
    (is (= (.getLonMax bounds) 180.0)))
  (let [bounds (make-bounding-box (make-location -90 -180) (make-location 90 180))]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) -180.0))
    (is (= (.getLonMax bounds) 180.0)))
  (let [bounds (make-bounding-box (make-location 90 180) (make-location -90 -180))]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) -180.0))
    (is (= (.getLonMax bounds) 180.0)))
  (let [bounds (make-bounding-box 90 180 -90 -180)]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) -180.0))
    (is (= (.getLonMax bounds) 180.0))))
