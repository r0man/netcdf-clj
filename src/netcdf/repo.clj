(ns netcdf.repo
  (:import java.io.File)
  (:use [clojure.string :only (join)]
        [netcdf.variable :only (download-variable variable-fragment)]
        netcdf.file
        netcdf.utils))

(def ^:dynamic *local-root*
  (str (System/getenv "HOME") File/separator ".netcdf"))

(defprotocol IRepository
  (reference-times [repository model]
    "Returns model's reference times in the repository.")
  (save-variable [repository model variable reference-time]
    "Save the variable in repository.")
  (variable-url [repository model variable reference-time]
    "Returns the uri to the variable."))

;; LOCAL REPOSITORY

(defn local-reference-times
  "Returns the reference times in the local repository."
  [repository model]
  (netcdf-file-seq (:url repository)))

(defn local-variable-url
  "Returns the variable url in the local repository."
  [repository model variable reference-time]
  (str (:url repository) File/separator
       (variable-fragment model variable reference-time)))

(defrecord LocalRepository [url]
  IRepository
  (reference-times [repository model]
    (local-reference-times repository model))
  (save-variable [repository model variable reference-time]
    (download-variable model variable :reference-time reference-time :root-dir (:url repository)))
  (variable-url [repository model variable time]
    (local-variable-url repository model variable time)))

(defn make-local-repository
  "Make a local repository."
  [& [url]] (LocalRepository. (or url *local-root*)))
