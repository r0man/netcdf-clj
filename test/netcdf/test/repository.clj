(ns netcdf.test.repository
  (:import java.io.File)
  (:use clojure.test
        netcdf.time
        netcdf.repository))

(deftest test-variable-directory
  (is (= (str *root-dir* "/htsgwsfc/2011/08/06/000000Z")
         (variable-directory {:name "htsgwsfc"} "2011-08-06")))
  (is (= (str *root-dir* "/htsgwsfc/2011/08/06/000000Z")
         (variable-directory {:name "htsgwsfc"} (to-date-time "2011-08-06"))))
  (is (= (str *root-dir* "/htsgwsfc/2011/08/06/120000Z")
         (variable-directory {:name "htsgwsfc"} (to-date-time "2011-08-06T12:00:00Z")))))

(deftest test-with-root-dir
  (is (= (str (System/getenv "HOME") File/separator ".netcdf") *root-dir*))
  (with-root-dir "/tmp"
    (is (= "/tmp" *root-dir*))))