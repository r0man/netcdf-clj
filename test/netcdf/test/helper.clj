(ns netcdf.test.helper
  (:import java.io.File)
  (:use [clj-time.core :only (date-time day days hours minus month now year)]
        clj-time.format
        clojure.contrib.logging)
  (:require [netcdf.dataset :as dataset]
            [netcdf.dods :as dods]))

(defn refer-private [ns]
  (doseq [[symbol var] (ns-interns ns)]
    (when (:private (meta var))
      (intern *ns* symbol var))))

(defmacro with-test-inventory [& body]
  `(let [inventory# (dods/find-inventory-by-url "test-resources/dods/wave/nww3")]
     (binding [dods/find-inventory-by-url (fn [url#] inventory#)]
       ~@body)))

(def *product* "nww3")
(def *variable* "htsgwsfc")

(def *valid-time* (minus (date-time (year (now)) (month (now)) (day (now))) (days 2)))

(def *dataset-uri* (str (System/getProperty "java.io.tmpdir") File/separator "netcdf-test.nc"))

(def *remote-uri*
     (str "http://nomads.ncep.noaa.gov:9090/dods/wave/" *product* "/"
          *product* (unparse (formatters :basic-date) *valid-time*) "/"
          *product* (unparse (formatters :basic-date) *valid-time*) "_"
          (unparse (formatters :hour) *valid-time*) "z"))

(if-not (.exists (File. *dataset-uri*))
  (do
    (info "Downloading test data:" *remote-uri*)
    (time (dataset/copy-dataset *remote-uri* *dataset-uri* [*variable*]))))

(def *dataset* (dataset/open-grid-dataset *dataset-uri*))
(def *nww3-htsgwsfc* (dataset/find-geo-grid *dataset* *variable*))