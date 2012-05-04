(ns netcdf.test.utils
  (:import java.io.File)
  (:use [clj-time.core :only (now in-secs interval date-time year month day hour)]
        clojure.test
        netcdf.repository
        netcdf.test.helper
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

(deftest test-nan?
  (is (not (nan? 0)))
  (is (nan? Double/NaN)))

(deftest test-with-meta+
  (let [obj [1 2] m {:key "val"}]
    (is (= (with-meta+ obj {}) obj))
    (is (= (with-meta+ obj m) obj))
    (is (= (meta (with-meta+ obj m)) m))
    (is (= (meta (with-meta+ (with-meta obj {:key "x"}) m)) m))
    (is (= (meta (with-meta+ (with-meta obj m) {:key2 "val2"})) (merge m {:key2 "val2"})))))

(deftest test-netcdf-file?
  (is (netcdf-file? example-path))
  (is (netcdf-file? (File. example-path)))
  (is (not (netcdf-file? "test")))
  (is (not (netcdf-file? (File. "test")))))

(deftest test-netcdf-file-seq
  (let [files (netcdf-file-seq *repository*)]
    (is (every? #(instance? File %1) files))
    (is (every? netcdf-file? files))))
