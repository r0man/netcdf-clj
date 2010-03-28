(ns netcdf.test.utils
  (:import java.io.File)
  (:use clojure.test netcdf.utils))

(deftest test-file-exists?
  (is (file-exists? (System/getProperty "user.home")))
  (is (not (file-exists? "not-existing-file"))))

(deftest test-file-extension
  (is (nil? (file-extension "filename")))
  (is (= (file-extension "test.png") "png"))
  (is (= (file-extension (File. "test.png")) "png")))

(deftest test-with-meta+
  (let [obj [1 2] m {:key "val"}]
    (is (= (with-meta+ obj {}) obj))
    (is (= (with-meta+ obj m) obj))
    (is (= (meta (with-meta+ obj m)) m))
    (is (= (meta (with-meta+ (with-meta obj {:key "x"}) m)) m))
    (is (= (meta (with-meta+ (with-meta obj m) {:key2 "val2"})) (merge m {:key2 "val2"})))))


