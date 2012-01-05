(ns netcdf.repository
  (:import java.io.File)
  (:use [clojure.string :only (join)]
        netcdf.time
        netcdf.utils
        netcdf.dods))

(def ^:dynamic *repository*
  (str (System/getenv "HOME") File/separator ".netcdf"))

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


;; (with-repository :local
;;   (read-forecast surf-forecast mundaka "2012-01-01"))

;; (with-repository :dods
;;   (read-forecast surf-forecast mundaka "2012-01-01"))
