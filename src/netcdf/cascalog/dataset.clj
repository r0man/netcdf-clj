(ns netcdf.cascalog.dataset
  (:require [clj-time.coerce :refer [to-date-time]])
  (:import netcdf.cascading.GridDatasetTap
           org.joda.time.DateTime))

(defn dataset [model url datatypes timestamps]
  (GridDatasetTap.
   (name model)
   (str url)
   (into-array String datatypes)
   (into-array DateTime (map to-date-time timestamps))))
