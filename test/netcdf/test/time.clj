(ns netcdf.test.time
  (:import java.sql.Timestamp)
  (:use [clj-time.core :only (date-time)]
        netcdf.time
        clojure.test))

(deftest test-date-path-fragment
  (is (nil? (date-path-fragment nil)))
  (is (= "2011/02/04" (date-path-fragment (date-time 2011 2 4))))
  (is (= "2011/02/04" (date-path-fragment "2011-02-04T14:39:05Z"))))

(deftest test-date-time-path-fragment
  (is (nil? (date-time-path-fragment nil)))
  (is (= "2010/02/14/143905Z" (date-time-path-fragment (date-time 2010 2 14 14 39 5))))
  (is (= "2010/02/14/143905Z" (date-time-path-fragment "2010-02-14T14:39:05Z"))))

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

(deftest test-time-path-fragment
  (is (nil? (time-path-fragment nil)))
  (is (= "143905Z" (time-path-fragment (date-time 2011 2 4 14 39 5))))
  (is (= "143905Z" (time-path-fragment "2010-02-14T14:39:05Z"))))