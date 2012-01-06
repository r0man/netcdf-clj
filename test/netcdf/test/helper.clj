(ns netcdf.test.helper
  (:import java.io.File)
  (:use [clj-time.core :only (date-time day days hours minus month now year)]
        [netcdf.forecast :only (defforecast download-forecast)]
        [netcdf.model :only (akw gfs-hd nww3)]
        [netcdf.variable :only (htsgwsfc tmpsfc)]
        clj-time.format
        clojure.tools.logging)
  (:require [netcdf.dataset :as dataset]
            [netcdf.dods :as dods]))

(defn refer-private [ns]
  (doseq [[symbol var] (ns-interns ns)]
    (when (:private (meta var))
      (intern *ns* symbol var))))

(defmacro with-test-inventory [& body]
  `(let [inventory# (dods/find-inventory-by-url "test-resources/dods/wave/nww3")]
     (with-redefs [dods/find-inventory-by-url (fn [url#] inventory#)]
       ~@body)))

(def example-valid-time
  (minus (date-time (year (now)) (month (now)) (day (now))) (days 2)))

(defforecast example-forecast
  "The example forecast."
  htsgwsfc [akw nww3]
  tmpsfc [gfs-hd])

(info (str "Downloading example forecast ..."))
(download-forecast example-forecast :reference-time example-valid-time)

(def example-product
  "nww3")

(def example-variable
  "htsgwsfc")

(def example-path
  (str (System/getProperty "java.io.tmpdir") File/separator "netcdf-test.nc"))

(def ^:dynamic *remote-uri*
  (str "http://nomads.ncep.noaa.gov:9090/dods/wave/" example-product "/"
       example-product (unparse (formatters :basic-date) example-valid-time) "/"
       example-product (unparse (formatters :basic-date) example-valid-time) "_"
       (unparse (formatters :hour) example-valid-time) "z"))

(if-not (.exists (File. example-path))
  (do
    (info (str "Downloading test data:" *remote-uri*))
    (dataset/copy-dataset *remote-uri* example-path [example-variable])))

(def example-dataset
  (dataset/open-grid-dataset example-path))

(def example-geo-grid
  (dataset/find-geo-grid example-dataset example-variable))
