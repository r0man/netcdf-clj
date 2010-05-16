(ns netcdf.test.helper
  (:import java.io.File)
  (:use clj-time.core clj-time.format)
  (:require [netcdf.datatype :as datatype]
            [netcdf.dataset :as dataset]))

(def *product* "nww3")
(def *variable* "htsgwsfc")

(def *valid-time* (minus (date-time (year (now)) (month (now)) (day (now))) (days 1)))

(def *dataset-uri* (str (System/getProperty "java.io.tmpdir") File/separator "netcdf-test.nc"))

(def *remote-uri*
     (str "http://nomad5.ncep.noaa.gov:9090/dods/waves/" *product* "/"
          *product* (unparse (formatters :basic-date) *valid-time*) "/"
          *product* "_" (unparse (formatters :hour) *valid-time*) "z"))

(if-not (.exists (File. *dataset-uri*))
  (do
    (println "Downloading test data:" *remote-uri*)
    (time (dataset/copy-dataset *remote-uri* *dataset-uri* [*variable*]))))

(def *datatype* (datatype/open-datatype (datatype/make-datatype *dataset-uri* *variable*)))

(def *dataset* (dataset/open-dataset *dataset-uri*))

;; *remote-uri*


;; (def *valid-time* (first (datatype/valid-times *datatype*)))

;; (time
;;  (copy-dataset *remote-uri* *dataset-uri* [*variable*]))


;; (def *remote-uri*
;;      "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3/nww320100515/nww3_00z")
