(ns netcdf.repo
  (:import java.io.File)
  (:use [clojure.string :only (join)]
        [netcdf.variable :only (variable-fragment)]
        netcdf.file
        netcdf.utils))

(def ^:dynamic *local-variable-root*
  (str (System/getenv "HOME") File/separator ".netcdf"))

(defprotocol IRepository
  (reference-times [repository model]
    "Returns model's reference times in the repository.")
  (variable-url [repository model variable reference-time]
    "Returns the uri to the variable."))

;; LOCAL REPOSITORY

(defrecord LocalRepository [url])

(defn local-reference-times
  "Returns the reference times in the local repository."
  [repository model]
  (netcdf-file-seq (:url repository)))

(defn local-variable-url
  "Returns the variable url in the local repository."
  [repository model variable reference-time]
  (str (:url repository) File/separator
       (variable-fragment model variable reference-time)))

(defn make-local-repository
  "Make a local repository."
  [url] (LocalRepository. url))

(extend-type LocalRepository
  IRepository
  (reference-times [repository model]
    (local-reference-times repository model))
  (variable-url [repository model variable time]
    (local-variable-url repository model variable time)))
