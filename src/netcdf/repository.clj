(ns netcdf.repository
  (:import java.io.File)
  (:require [netcdf.dods :as dods]
            [netcdf.geo-grid :as grid])
  (:use [clojure.string :only (join)]
        [netcdf.time :only (parse-fragment)]
        netcdf.file
        netcdf.utils))

(def ^:dynamic *repository* nil)

(def ^:dynamic *local-root*
  (str (System/getenv "HOME") File/separator ".netcdf"))

(defprotocol IRepository
  (-dataset-url [repository model variable reference-time]
    "Returns the url to the dataset in repository.")
  (-open-grid [repository model variable reference-time]
    "Open the grid for the variable from repository.")
  (-reference-times [repository model]
    "Returns model's reference times in repository."))

(defn dataset-url
  "Returns the dataset url in *repository*."
  [model variable reference] (-dataset-url *repository* model variable reference))

(defn open-grid
  "Open the geo grid in *repository*."
  [model variable reference] (-open-grid *repository* model variable reference))

(defn reference-times
  "Returns reference times of model in *repository*."
  [model] (-reference-times *repository* model))

(defmacro with-repository
  "Bind *repository* to directory and evaluate body."
  [directory & body]
  `(binding [*repository* ~directory]
     ~@body))

;; LOCAL REPOSITORY

(defn local-reference-times
  "Returns the reference times in the local repository."
  [repository model]
  (->> (netcdf-file-seq (:url repository))
       (map parse-fragment)
       (remove nil?)
       (sort)))

(defn local-dataset-url
  "Returns the variable url in the local repository."
  [model variable reference-time & [root-dir]]
  (->> [(or root-dir *local-root*)
        (:name model)
        (:name variable)
        (str (date-time-path-fragment reference-time) ".nc")]
       (join File/separator)))

(defrecord LocalRepository [url]
  IRepository
  (-dataset-url [repository model variable reference-time]
    (local-dataset-url model variable reference-time (:url repository)))
  (-open-grid [repository model variable reference-time]
    (grid/open-geo-grid
     (-dataset-url repository model variable reference-time)
     (:name variable)))
  (-reference-times [repository model]
    (local-reference-times repository model)))

(defn make-local-repository
  "Make a local repository."
  [& [url]] (LocalRepository. (or url *local-root*)))

;; DODS REPOSITORY

(defn dods-dataset-url
  "Returns the DODS dataset url."
  [model variable reference-time]
  (:dods (first (dods/find-datasets-by-url-and-reference-time (:dods model) reference-time))))

(defrecord DodsRepository []
  IRepository
  (-dataset-url [repository model variable reference-time]
    (dods-dataset-url model variable reference-time))
  (-open-grid [repository model variable reference-time]
    (grid/open-geo-grid
     (-dataset-url repository model variable reference-time)
     (:name variable)))
  (-reference-times [repository model]
    (dods/reference-times model)))

(defn make-dods-repository
  "Make a DODS repository."
  [] (DodsRepository.))

(alter-var-root #'*repository* (constantly (make-local-repository)))
