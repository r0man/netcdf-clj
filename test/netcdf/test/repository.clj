(ns netcdf.test.repository
  (:import java.io.File java.net.URI)
  (:use [clj-time.core :only (date-time)]
        [netcdf.model :only (akw)]
        [netcdf.variable :only (htsgwsfc)]
        clojure.test
        netcdf.time
        netcdf.repository))

(deftest test-local-variable-path
  (is (= (local-variable-path akw htsgwsfc (date-time 2010 11 5 6))
         (URI. (str "file:" *repository* "/akw/htsgwsfc/2010/11/05/060000Z.nc"))))
  (with-repository "/tmp"
    (is (= (local-variable-path akw htsgwsfc (date-time 2010 11 5 6))
           (URI. "file:/tmp/akw/htsgwsfc/2010/11/05/060000Z.nc")))))

(deftest test-variable-path
  (is (= (str *repository* "/akw/htsgwsfc/2010/11/05/060000Z.nc")
         (variable-path akw htsgwsfc "2010-11-05T06:00:00Z")))
  (is (= (str *repository* "/akw/htsgwsfc/2010/11/05/060000Z.nc")
         (variable-path akw htsgwsfc (date-time 2010 11 5 6))))
  (with-repository "/tmp"
    (is (= "/tmp/akw/htsgwsfc/2010/11/05/060000Z.nc"
           (variable-path akw htsgwsfc (date-time 2010 11 5 6)))))
  (with-repository "s3n://burningswell/netcdf"
    (is (= "s3n://burningswell/netcdf/akw/htsgwsfc/2010/11/05/060000Z.nc"
           (variable-path akw htsgwsfc (date-time 2010 11 5 6))))))

(deftest test-with-repository
  (is (= (str (System/getenv "HOME") File/separator ".netcdf") *repository*))
  (with-repository "/tmp" (is (= "/tmp" *repository*))))
