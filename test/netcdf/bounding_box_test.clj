(ns netcdf.bounding-box-test
  (:use clojure.test
        netcdf.bounding-box
        netcdf.location))

(deftest test-to-bounding-box
  (let [bounds (make-bounding-box 75.25 -123.5 44.75 159.5)]
    (is (= bounds (to-bounding-box bounds))))
  (let [bounds (to-bounding-box {:south-west { :latitude 75.25 :longitude -123.5} :north-east { :latitude 44.75 :longitude 159.5}})]
    (is (= (.getLatMin bounds) 44.75))
    (is (= (.getLatMax bounds) 75.25))
    (is (= (.getLonMin bounds) -123.5))
    (is (= (.getLonMax bounds) 159.5))))

(deftest test-contains-location?
  (let [bounds (make-bounding-box 75.25 -123.5 44.75 159.5)]
    (is (contains-location? bounds (make-location 75.25 -123.5)))
    (is (contains-location? bounds (make-location 44.75 159.5)))))

(deftest test-make-bounding
  (let [bounds (make-bounding-box (make-location -90 -180) (make-location 90 180))]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) -180.0))
    (is (= (.getLonMax bounds) 180.0)))
  (let [bounds (make-bounding-box (make-location 90 180) (make-location -90 -180))]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) 180.0))
    (is (= (.getLonMax bounds) 540.0)))
  (let [bounds (make-bounding-box 90 180 -90 -180)]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) 180.0))
    (is (= (.getLonMax bounds) 540.0))))

(deftest test-parse-bounding
  (let [bounds (parse-bounding-box "-90,-180,90,180")]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) -180.0))
    (is (= (.getLonMax bounds) 180.0)))
  (let [bounds (parse-bounding-box "90,180 -90,-180")]
    (is (= (.getLatMin bounds) -90.0))
    (is (= (.getLatMax bounds) 90.0))
    (is (= (.getLonMin bounds) 180.0))
    (is (= (.getLonMax bounds) 540.0))))
