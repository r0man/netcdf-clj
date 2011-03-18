(ns netcdf.test.utils
  (:import java.io.File)
  (:use [clj-time.core :only (now in-secs interval date-time year month day hour)]
        clojure.test
        netcdf.utils))

(deftest test-file-exists?
  (is (file-exists? (System/getProperty "user.home")))
  (is (not (file-exists? "not-existing-file"))))

(deftest test-file-extension
  (is (nil? (file-extension "filename")))
  (is (= (file-extension "test.png") "png"))
  (is (= (file-extension (File. "test.png")) "png")))

(deftest test-file-size
  (is (nil? (file-size nil)))
  (is (= 4757 (file-size "test-resources/dods/xml"))))

(deftest test-human-duration
  (is (= "0 s" (human-duration (interval (now) (now)))))
  (is (= "1 s" (human-duration (interval (date-time 2010 12 18 0 0 0) (date-time 2010 12 18 0 0 1))))))

(deftest test-human-file-size
  (is (= "4757 bytes" (human-file-size "test-resources/dods/xml"))))

(deftest test-human-transfer-rate
  (is (= "0.0 KB/s" (human-transfer-rate  0 (interval (now) (now)))))
  (is (= "1.0 KB/s" (human-transfer-rate 1000 (interval (date-time 2010 12 18 0 0 0) (date-time 2010 12 18 0 0 1))))))

(deftest test-with-meta+
  (let [obj [1 2] m {:key "val"}]
    (is (= (with-meta+ obj {}) obj))
    (is (= (with-meta+ obj m) obj))
    (is (= (meta (with-meta+ obj m)) m))
    (is (= (meta (with-meta+ (with-meta obj {:key "x"}) m)) m))
    (is (= (meta (with-meta+ (with-meta obj m) {:key2 "val2"})) (merge m {:key2 "val2"})))))

(deftest test-date-path-fragment
  (is (nil? (date-path-fragment nil)))
  (is (= "2011/02/04" (date-path-fragment (date-time 2011 2 4))))
  (is (= "2011/02/04" (date-path-fragment "2011-02-04T14:39:05Z"))))

(deftest test-time-path-fragment
  (is (nil? (time-path-fragment nil)))
  (is (= "143905Z" (time-path-fragment (date-time 2011 2 4 14 39 5))))
  (is (= "143905Z" (time-path-fragment "2010-02-14T14:39:05Z"))))

(deftest test-date-time-path-fragment
  (is (nil? (date-time-path-fragment nil)))
  (is (= "2010/02/14/143905Z" (date-time-path-fragment (date-time 2010 2 14 14 39 5))))
  (is (= "2010/02/14/143905Z" (date-time-path-fragment "2010-02-14T14:39:05Z"))))

