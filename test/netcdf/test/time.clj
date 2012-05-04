(ns netcdf.test.time
  (:import java.sql.Timestamp)
  (:use [clj-time.core :only (date-time)]
        netcdf.time
        clojure.test))

(deftest test-date-time?
  (is (not (date-time? nil)))
  (is (not (date-time? "")))
  (is (date-time? (date-time 2011 1 1))))

(deftest test-format-time
  (testing "nil"
    (is (nil? (format-time nil))))
  (testing "java.lang.String"
    (is (= "2011-01-31T08:35:53Z" (format-time "2011-01-31T08:35:53Z"))))
  (testing "java.sql.Timestamp"
    (is (= "2011-01-31T08:35:53Z" (format-time (Timestamp. 1296462953000)))))
  (testing "org.joda.time.DateTime"
    (is (= "2011-01-31T08:35:53Z" (format-time (date-time 2011 1 31 8 35 53))))))

(deftest test-parse-fragment
  (are [fragment expected]
    (is (= expected (parse-fragment fragment)))
    "2012/01/04/000000Z" (date-time 2012 1 4)
    "/home/roman/.netcdf/gfs-hd/tmpsfc/2012/01/04/000000Z.nc" (date-time 2012 1 4)
    "http://example.com/netcdf/gfs-hd/tmpsfc/2012/01/04/000000Z.nc" (date-time 2012 1 4)
    "s3://my-bucket/netcdf/gfs-hd/tmpsfc/2012/01/04/000000Z.nc" (date-time 2012 1 4)))
