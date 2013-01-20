(ns netcdf.repository
  (:refer-clojure :exclude (replace))
  (:import java.io.File)
  (:require [netcdf.dods :as dods]
            [netcdf.geo-grid :as grid])
  (:use [clojure.string :only (join replace)]
        [clj-time.core :only (year month day hour)]
        [clj-time.coerce :only (to-date)]
        [clj-time.format :only (parse unparse formatters)]
        netcdf.time
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
       (apply sorted-set)))

(defn local-dataset-url
  "Returns the variable url in the local repository."
  [model variable reference-time & [root-dir]]
  (format "%s/%4d/%02d/%02d/%02d/%s/%s/%s-%s-%s.nc"
          (or root-dir *local-root*)
          (year reference-time)
          (month reference-time)
          (day reference-time)
          (hour reference-time)
          (:name model)
          (:name variable)
          (:name model)
          (:name variable)
          (unparse (formatters :date-hour) reference-time)))

(defrecord LocalRepository [url]
  IRepository
  (-dataset-url [repository model variable reference-time]
    (local-dataset-url model variable reference-time (:url repository)))
  (-open-grid [repository model variable reference-time]
    (let [url (-dataset-url repository model variable reference-time)]
      (if (file-exists? url)
        (grid/open-geo-grid url (:name variable)))))
  (-reference-times [repository model]
    (local-reference-times repository model)))

(defn make-local-repository
  "Make a local repository."
  [& [url]] (LocalRepository. (or url *local-root*)))

;; DODS REPOSITORY

(defn dods-dataset-url
  "Returns the DODS dataset url."
  [model variable reference-time]
  (:dods (dods/datasource model reference-time)))

(defrecord DodsRepository []
  IRepository
  (-dataset-url [repository model variable reference-time]
    (dods-dataset-url model variable reference-time))
  (-open-grid [repository model variable reference-time]
    (try
      (grid/open-geo-grid
       (-dataset-url repository model variable reference-time)
       (:name variable))
      (catch java.io.FileNotFoundException _ nil)))
  (-reference-times [repository model]
    (map :reference-time (dods/datasources model))))

(defn make-dods-repository
  "Make a DODS repository."
  [] (DodsRepository.))

;; DISTRIBUTED CACHE

(defn- parse-dist-cache-path [path]
  (let [[_ model variable reference-time]
        (re-find #"([^$]+)\$([^$]+)\$([^$]+).nc" (.getName (File. (str path))))
        reference-time (parse reference-time)]
    (if (and model variable reference-time)
      {:model model :variable variable :reference-time reference-time})))

(defn dist-cache-dataset-url
  "Returns the dataset url in the distributed cache."
  [model variable reference-time & [root]]
  (->> [(:name model)
        (:name variable)
        (str (format-time reference-time) ".nc")]
       (remove nil?)
       (join "$")
       (str (if root (str root File/separator)))))

(defn dist-cache-reference-times
  "Returns the url of the dataset in the distributed cache."
  [repository model]
  (->> (netcdf-file-seq (:url repository))
       (map parse-dist-cache-path)
       (filter #(= (:name model) (:model %1)))
       (map :reference-time)
       (apply sorted-set)))

(defrecord DistCacheRepository [url]
  IRepository
  (-dataset-url [repository model variable reference-time]
    (dist-cache-dataset-url model variable reference-time (:url repository)))
  (-open-grid [repository model variable reference-time]
    (let [url (-dataset-url repository model variable reference-time)]
      (if (file-exists? url)
        (grid/open-geo-grid url (:name variable)))))
  (-reference-times [repository model]
    (dist-cache-reference-times repository model)))

(defn make-dist-cache-repository
  "Make a distributed cache repository."
  [& [url]] (DistCacheRepository. (or url "/var/lib/hadoop/mapred")))

(alter-var-root #'*repository* (constantly (make-local-repository)))
