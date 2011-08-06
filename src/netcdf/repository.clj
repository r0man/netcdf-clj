(ns netcdf.repository
  (:import java.io.File)
  (:use [clojure.contrib.def :only (defvar)]
        [clojure.string :only (join)]
        netcdf.time
        netcdf.utils
        netcdf.dods))

(defvar *root-dir*
  (str (System/getenv "HOME") File/separator ".netcdf")
  "The local NetCDF directory.")

(defn variable-directory [variable & [reference-time root-dir]]
  (let [reference-time (to-date-time reference-time)]
    (join File/separator
          [(or root-dir *root-dir*) (:name variable)
           (date-time-path-fragment reference-time)])))

(defn variable-path [model variable & [reference-time root-dir]]
  (let [reference-time (to-date-time (or reference-time (latest-reference-time model)))]
    (join File/separator
          [(or root-dir *root-dir*) (:name variable)
           (date-time-path-fragment reference-time)
           (str (:name model) ".nc")])))

(defmacro with-root-dir
  "Bind *root-dir* to directory and evaluate body."
  [directory & body]
  `(binding [*root-dir* ~directory]
     ~@body))
