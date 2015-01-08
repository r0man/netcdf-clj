(ns netcdf.cascalog.dataset-test
  (:require [clj-time.coerce :refer [to-date-time]]
            [cascalog.api :refer :all]
            [clojure.test :refer :all]
            [netcdf.cascalog.dataset :refer :all]))

(def model "akw")

(def url "test-resources/akw-htsgwsfc-2014-01-14T00.nc")

(def datatypes
  ["htsgwsfc"])

(def timestamps
  (map to-date-time ["2014-01-22T16:00:00.000+01:00"]))

(deftest test-dataset
  (let [dataset (dataset model url datatypes timestamps)]
    (let [scheme (.getScheme dataset)]
      (is (= model (.getModel scheme)))
      (is (= url (.getUrl scheme)))
      (is (= datatypes (seq (.getDatatypes scheme))))
      (is (= timestamps (seq (.getTimestamps scheme)))))
    (is (= ["model" "datatype" "timestamp" "latitude" "longitude" "value" "unit" "width" "height" "x" "y"]
           (seq (.getSourceFields dataset))))))

(comment
  (deftest test-query-dataset
   (?- (hfs-textline "/tmp/test-query-dataset" :sinkmode :replace)
       (dataset model url nil nil))))

(comment
  (?- (stdout)
      (dataset model url datatypes timestamps)))
