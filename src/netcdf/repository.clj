(ns netcdf.repository
  (:import java.io.File)
  (:use [clojure.string :only (join)]
        netcdf.time
        netcdf.utils
        netcdf.dods))

(def ^:dynamic *repository*
  (str (System/getenv "HOME") File/separator ".netcdf"))

(defn variable-path
  "Returns the path to the NetCDF variable file."
  [model variable & [reference-time]]
  (->> [*repository*
        (:name model)
        (:name variable)
        (str (date-time-path-fragment reference-time) ".nc")]
       (join File/separator)))

(defn local-variable-path
  "Returns the path to the local NetCDF variable file."
  [model variable & [reference-time]]
  (java.net.URI. (str "file:" (variable-path model variable reference-time))))

(defmacro with-repository
  "Bind *repository* to directory and evaluate body."
  [directory & body]
  `(binding [*repository* ~directory]
     ~@body))
