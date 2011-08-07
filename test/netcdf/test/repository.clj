(ns netcdf.test.repository
  (:import java.io.File java.net.URI)
  (:use [clj-time.core :only (date-time)]
        clojure.test
        netcdf.time
        netcdf.model
        netcdf.variable
        netcdf.repository))

(deftest test-local-variable-path
  (is (= (local-variable-path akw htsgwsfc (date-time 2010 11 5 6))
         (URI. (str "file:" *repository* "/htsgwsfc/2010/11/05/060000Z/akw.nc"))))
  (with-repository "/tmp"
    (is (= (local-variable-path akw htsgwsfc (date-time 2010 11 5 6))
           (URI. "file:/tmp/htsgwsfc/2010/11/05/060000Z/akw.nc")))))

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