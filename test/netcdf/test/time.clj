(ns netcdf.test.time
  (:import java.util.Calendar java.util.Date org.joda.time.DateTime)
  (:use [clj-time.core :only (date-time now)]
        [clj-time.coerce :only (to-long)]
        netcdf.time
        clojure.test))

(deftest test-to-calendar
  (let [calendar (to-calendar 0)]
    (is (instance? Calendar calendar))
    (is (= 0 (.getTime (.getTime calendar))))))

(deftest test-to-date-time
  (testing "nil"
    (is (nil? (to-date-time nil))))
  (testing "java.lang.Number"
    (is (= (date-time 1970 1 1) (to-date-time 0)))))

(deftest test-to-ms
  (testing "nil"
    (is (nil? (to-ms nil))))
  (testing "java.lang.Number"
    (is (= 0 (to-ms 0)))
    (is (= 1298408514294 (to-ms 1298408514294))))
  (testing "java.lang.String"
    (is (= 0 (to-ms "1970-01-01T00:00:00Z")))
    (is (= 0 (to-ms "1970-01-01")))
    (is (= 1298408514000 (to-ms "2011-02-22T21:01:54Z")))
    (is (= 1298332800000 (to-ms "2011-02-22"))))
  (testing "java.util.Calendar"
    (is (= 0 (to-ms (to-calendar 0))))
    (is (= 1298408514294 (to-ms (to-calendar 1298408514294)))))
  (testing "java.util.Date"
    (is (= 0 (to-ms (Date. (long 0)))))
    (is (= 1298408514294 (to-ms (Date. (long 1298408514294))))))
  (testing "java.sql.Date"
    (is (= 0 (to-ms (java.sql.Date. 0))))
    (is (= 1298408514294 (to-ms (java.sql.Date. 1298408514294)))))
  (testing "java.sql.Timestamp"
    (is (= 0 (to-ms (java.sql.Timestamp. 0))))
    (is (= 1298408514294 (to-ms (java.sql.Timestamp. 1298408514294)))))
  (testing "org.joda.time.DateTime"
    (is (= 0 (to-ms (DateTime. (long 0)))))
    (is (= 1298408514294 (to-ms (DateTime. (long 1298408514294)))))))

(deftest test-date-time?
  (is (not (date-time? nil)))
  (is (not (date-time? "")))
  (is (date-time? (date-time 2011 1 1))))

(deftest test-format-date
  (testing "nil"
    (is (nil? (format-date nil))))
  (testing "java.lang.String"
    (is (= "2011-01-31" (format-date "2011-01-31"))))
  (testing "java.util.Date"
    (is (= "2011-01-31" (format-date (java.util.Date. 1296462953000)))))
  (testing "java.sql.Date"
    (is (= "2011-01-31" (format-date (java.sql.Date. 1296462953000)))))
  (testing "java.sql.Timestamp"
    (is (= "2011-01-31" (format-date (java.sql.Timestamp. 1296462953000)))))
  (testing "org.joda.time.DateTime"
    (is (= "2011-01-31" (format-date (date-time 2011 1 31 8 35 53))))))

(deftest test-format-time
  (testing "nil"
    (is (nil? (format-time nil))))
  (testing "java.lang.String"
    (is (= "2011-01-31T08:35:53Z" (format-time "2011-01-31T08:35:53Z"))))
  (testing "java.sql.Timestamp"
    (is (= "2011-01-31T08:35:53Z" (format-time (java.sql.Timestamp. 1296462953000)))))
  (testing "org.joda.time.DateTime"
    (is (= "2011-01-31T08:35:53Z" (format-time (date-time 2011 1 31 8 35 53))))))

(deftest test-parse-fragment
  (are [fragment expected]
    (is (= expected (parse-fragment fragment)))
    "2012/01/04/000000Z" (date-time 2012 1 4)
    "/home/roman/.netcdf/gfs-hd/tmpsfc/2012/01/04/000000Z.nc" (date-time 2012 1 4)
    "http://example.com/netcdf/gfs-hd/tmpsfc/2012/01/04/000000Z.nc" (date-time 2012 1 4)
    "s3://my-bucket/netcdf/gfs-hd/tmpsfc/2012/01/04/000000Z.nc" (date-time 2012 1 4)))

(deftest test-sql-timestamp
  (testing "nil"
    (is (nil? (sql-timestamp nil))))
  (testing "java.lang.String"
    (is (= (java.sql.Timestamp. 1296462953000) (sql-timestamp "2011-01-31T08:35:53Z"))))
  (testing "java.util.Date"
    (is (= (java.sql.Timestamp. 1296462953000) (sql-timestamp (java.util.Date. 1296462953000)))))
  (testing "org.joda.time.DateTime"
    (is (= (java.sql.Timestamp. 1296462953000) (sql-timestamp (date-time 2011 1 31 8 35 53))))))

(deftest test-sql-timestamp-now
  (is (instance? java.sql.Timestamp (sql-timestamp-now)))
  (= (.getTime (sql-timestamp-now)) (to-long (now))))
