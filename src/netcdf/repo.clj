(ns netcdf.repo
  (:use netcdf.file))

(defprotocol IRepository
  (reference-times [repository model]))

;; LOCAL REPOSITORY

(defrecord LocalRepository [url])

(defn local-reference-times [repository model]
  (netcdf-file-seq (:url repository)))

(defn make-local-repository
  "Make a local repository."
  [url] (LocalRepository. url))

(extend-type LocalRepository
  IRepository
  (reference-times [repository model]
    (local-reference-times repository model)))
