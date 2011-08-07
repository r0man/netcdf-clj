(ns netcdf.repository
  (:import java.io.File)
  (:use [clojure.contrib.def :only (defvar)]
        [clojure.string :only (join)]
        netcdf.time
        netcdf.utils
        netcdf.dods))

(defvar *repository*
  (str (System/getenv "HOME") File/separator ".netcdf")
  "The local NetCDF directory.")

(defn variable-directory
  "Returns the path to the NetCDF directory."
  [variable reference-time]
  (join File/separator [*repository* (:name variable) (date-time-path-fragment reference-time)]))

(defn variable-path
  "Returns the path to the NetCDF variable file."
  [model variable & [reference-time]]
  (str (variable-directory variable (or reference-time (latest-reference-time model)))
       File/separator (:name model) ".nc"))

(defn local-variable-path
  "Returns the path to the local NetCDF variable file."
  [model variable & [reference-time]]
  (java.net.URI. (str "file:" (variable-path model variable reference-time))))

(defmacro with-repository
  "Bind *repository* to directory and evaluate body."
  [directory & body]
  `(binding [*repository* ~directory]
     ~@body))
