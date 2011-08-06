(ns netcdf.test.repository
  (:import java.io.File)
  (:use clojure.test
        netcdf.time
        netcdf.repository))

(deftest test-variable-directory
  (is (= (str *repository* "/htsgwsfc/2011/08/06/000000Z")
         (variable-directory {:name "htsgwsfc"} "2011-08-06")))
  (is (= (str *repository* "/htsgwsfc/2011/08/06/000000Z")
         (variable-directory {:name "htsgwsfc"} (to-date-time "2011-08-06"))))
  (is (= (str *repository* "/htsgwsfc/2011/08/06/120000Z")
         (variable-directory {:name "htsgwsfc"} (to-date-time "2011-08-06T12:00:00Z")))))

(deftest test-with-repository
  (is (= (str (System/getenv "HOME") File/separator ".netcdf") *repository*))
  (with-repository "/tmp"
    (is (= "/tmp" *repository*))))