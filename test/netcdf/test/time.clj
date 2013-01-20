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
       "/tmp/netcdf/2013/01/19/00/nww3/htsgwsfc/nww3-htsgwsfc-2013-01-19T00.nc"
       (date-time 2013 1 19)))

(deftest test-time-path-fragment
  (is (nil? (time-path-fragment nil)))
  (is (= "143905Z" (time-path-fragment (date-time 2011 2 4 14 39 5))))
  (is (= "143905Z" (time-path-fragment "2010-02-14T14:39:05Z"))))
