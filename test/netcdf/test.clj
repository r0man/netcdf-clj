(ns netcdf.test
  (:import java.io.File)
  (:use [clj-time.core :only (date-time day days hours minus month now year)]
        [netcdf.model :only (akw gfs-hd nww3)]
        [netcdf.variable :only (download-variable htsgwsfc tmpsfc)]
        [netcdf.repository :only (local-dataset-url)]
        clj-time.format
        clojure.tools.logging)
  (:require [netcdf.dataset :as dataset]
            [netcdf.dods :as dods]))

(defn refer-private [ns]
  (doseq [[symbol var] (ns-interns ns)]
    (when (:private (meta var))
      (intern *ns* symbol var))))

(defmacro with-test-inventory [& body]
  `(let [inventory# (dods/parse-inventory "test-resources/dods/wave/nww3")]
     (with-redefs [dods/parse-inventory (fn [url#] inventory#)]
       ~@body)))

(def example-reference-time
  (minus (date-time (year (now)) (month (now)) (day (now))) (days 1)))

(defonce example-variable
  (download-variable nww3 htsgwsfc :reference-time example-reference-time))

(def example-variable
  "htsgwsfc")

(def example-path
  (local-dataset-url nww3 htsgwsfc example-reference-time))

(def example-dataset
  (dataset/open-grid-dataset example-path))

(def example-geo-grid
  (dataset/find-geo-grid example-dataset (:name htsgwsfc)))
