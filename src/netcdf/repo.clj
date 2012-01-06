(ns netcdf.repo
  (:import java.io.File)
  (:require [netcdf.dods :as dods]
            [netcdf.geo-grid :as grid])
  (:use [clojure.string :only (join)]
        [netcdf.variable :only (download-variable variable-fragment)]
        netcdf.file
        netcdf.utils))

(def ^:dynamic *local-root*
  (str (System/getenv "HOME") File/separator ".netcdf"))

(defprotocol IRepository
  (reference-times [repository model]
    "Returns model's reference times in the repository.")
  (open-grid [repository model variable reference-time]
    "Open the grid for the variable from repository."))

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
  (open-grid [repository model variable reference-time]
    (grid/open-geo-grid
     (local-variable-url repository model variable reference-time)
     (:name variable))))

(defn make-local-repository
  "Make a local repository."
  [& [url]] (LocalRepository. (or url *local-root*)))

;; ;; DODS REPOSITORY

;; (defrecord DodsRepository []
;;   IRepository
;;   (reference-times [repository model]
;;     (dods/reference-times model))
;;   (variable-url [repository model variable time]
;;     (dods/variable-url repository model variable time)))

;; (defn make-dods-repository
;;   "Make a DODS repository."
;;   [] (DodsRepository.))
