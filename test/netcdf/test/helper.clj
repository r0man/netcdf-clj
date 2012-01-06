(ns netcdf.test.helper
  (:import java.io.File)
  (:use [clj-time.core :only (date-time day days hours minus month now year)]
        [netcdf.forecast :only (defforecast download-forecast)]
        [netcdf.model :only (akw gfs-hd nww3)]
        [netcdf.variable :only (htsgwsfc tmpsfc)]
        [netcdf.repository :only (variable-path)]
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

(defforecast example-forecast
  "The example forecast."
  htsgwsfc [akw nww3]
  tmpsfc [gfs-hd])

(def example-reference-time
  (minus (date-time (year (now)) (month (now)) (day (now))) (days 2)))

(def example-product
  "nww3")

(def example-variable
  "htsgwsfc")

(def example-path
  (variable-path nww3 htsgwsfc example-reference-time))

(def example-dataset
  (dataset/open-grid-dataset example-path))

(def example-geo-grid
  (dataset/find-geo-grid example-dataset (:name htsgwsfc)))

(download-forecast example-forecast :reference-time example-reference-time)
