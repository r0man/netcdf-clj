(ns netcdf.test.time
  (:use [clj-time.core :only (date-time)]
        clojure.test netcdf.time))

(deftest test-parse-time
  (is (= (parse-time "20100607T000000Z")
         (date-time 2010 6 7))))

(deftest test-format-time
  (is (= (format-time (date-time 2010 6 7))
         "20100607T000000Z")))
