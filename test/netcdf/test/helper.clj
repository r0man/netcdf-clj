(ns netcdf.test.helper
  (:import java.io.File)
  (:use [clj-time.core :only (date-time year month day days hours minus now)]
        clj-time.format)
  (:require [netcdf.dods :as dods]
            [netcdf.datatype :as datatype]
            [netcdf.dataset :as dataset]))

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

;; *remote-uri*

;; (if-not (.exists (File. *dataset-uri*))
;;   (do
;;     (println "Downloading test data:" *remote-uri*)
;;     (time (dataset/copy-dataset *remote-uri* *dataset-uri* [*variable*]))))

;; (def *datatype* (datatype/open-datatype (datatype/make-datatype *dataset-uri* *variable*)))

(def *dataset* (dataset/open-dataset *dataset-uri*))
