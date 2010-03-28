(ns netcdf.dods
  (:import java.util.Calendar java.io.File java.io.File java.net.URI)
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            clojure.contrib.zip-filter
            [netcdf.dataset :as dataset])
  (:use clojure.contrib.str-utils
        clojure.contrib.zip-filter.xml
        incanter.chrono
        [clojure.contrib.str-utils2 :only (join)]))

(def *local-root* (str (System/getProperty "user.home") File/separator ".netcdf"))

(defstruct repository :name :root :description)

(defn inventory-url [repository]
  (str (:root repository) "/xml"))

(defn make-repository [name root & [description]]
  (struct repository name root description))

(defn valid-time->reference-time [valid-time]
  (date (valid-time :year)
        (valid-time :month)
        (valid-time :day)
        (* (int (/ (valid-time :hour) 6)) 6)
        0))

(defn dataset-directory [repository valid-time]
  (str (:name repository) (format-date (valid-time->reference-time valid-time) "yyyyMMdd")))

(defn dataset-filename [repository valid-time]
  (str (:name repository) "_" (format-date (valid-time->reference-time valid-time) "HH") "z"))

(defn dataset-url [repository valid-time]
  (str (:root repository) "/" (dataset-directory repository valid-time) "/" (dataset-filename repository valid-time)))

(defn dataset-url->time [uri]
  (if-let [[_ year month day hour] (re-find #".*(\d{4})(\d{2})(\d{2})/.*(\d{2})z?.*$" (str uri))]
    (parse-date (str year month day hour) "yyyyMMddHH")))

(defn- feed-to-zip [uri]
  (zip/xml-zip (xml/parse uri)))

(defn parse-dods [xml]
  (xml-> (feed-to-zip xml) :dataset :dods text))

(defn parse-reference-times [xml root]
  (map dataset-url->time (sort (filter #(. % startsWith root) (parse-dods xml)))))

(defn reference-times [repository]
  (parse-reference-times (inventory-url repository) (:root repository)))

(defn latest-reference-time [repository]
  (last (reference-times repository)))

(defn local-uri [repository valid-time & [variable]]
  (let [reference-time (valid-time->reference-time valid-time)]
    (URI. (str "file://" *local-root* File/separator (:name repository) File/separator
               (if variable (str variable File/separator))
               (format-date reference-time "yyyyMMdd") File/separator "t" (format-date reference-time "HH") "z.nc"))))

(defn download-variable [repository variable & [reference-time]]
  (let [reference-time (or reference-time (latest-reference-time repository))
        uri (local-uri repository reference-time variable)]
    (if-not (.exists (java.io.File. uri))
      (dataset/copy-dataset (dataset-url repository reference-time) uri [variable]))
    uri))
