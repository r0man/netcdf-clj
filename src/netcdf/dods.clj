(ns netcdf.dods
  (:refer-clojure :exclude (replace))
  (:import java.util.Calendar java.io.File java.io.File java.net.URI)
  (:require [clojure.core.memoize :refer [ttl]]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [netcdf.dataset :as dataset])
  (:use [clj-time.coerce :only (to-date-time)]
        [clj-time.core :only (after? date-time day hour month year now )]
        clj-time.format
        [clojure.data.zip.xml :only (xml-> text)]
        [clojure.string :only (join replace)]
        clojure.tools.logging
        netcdf.utils))

(defn- feed-to-zip [uri]
  (zip/xml-zip (xml/parse uri)))

(defn inventory-url
  "Returns the url of the xml inventory."
  [url] (replace (str url) #"/dods.*" "/dods/xml"))

(defn parse-reference-time [uri]
  (if-let [[_ year month day hour] (re-find #".*(\d{4})(\d{2})(\d{2})/.*(\d{2})z?.*$" (str uri))]
    (try
      (date-time
       (parse-integer year)
       (parse-integer month)
       (parse-integer day)
       (parse-integer hour))
      (catch org.joda.time.IllegalFieldValueException _ nil))))

(defn- rewrite-https
  "Rewrite the DODS XML inventory url. TODO: Remove when nomads urls are fixed."
  [url]
  (some-> url (str/replace #"http://nomads.ncep.noaa.gov:\d+" "https://nomads.ncep.noaa.gov")))

(defn parse-inventory* [url]
  (debug (str "Parsing DODS inventory " url " ..."))
  (let [extract (fn [node selector] (first (xml-> node selector text)))]
    (for [dataset (xml-> (feed-to-zip (inventory-url url)) :dataset)]
      {:name (extract dataset :name)
       :description (extract dataset :description)
       :das (rewrite-https (extract dataset :das))
       :dds (rewrite-https (extract dataset :dds))
       :dods (rewrite-https (extract dataset :dods))
       :reference-time (parse-reference-time (extract dataset :dods))})))

(def parse-inventory
  ;; Cache inventories for 15 minutes.
  (ttl parse-inventory* :ttl/threshold (* 15 60 1000)))

(defn- compile-pattern [model]
  (cond
    (instance? java.util.regex.Pattern (:pattern model))
    (:pattern model)
    (string? (:pattern model))
    (re-pattern (:pattern model))
    :else #".*"))

(defn datasources
  "Returns the datasources of `model`."
  [model]
  (let [url (:dods model)
        pattern (compile-pattern model)]
    (->> (filter #(and (:dods %)
                       (.startsWith (:dods %) url)
                       (re-matches pattern (:dods %)))
                 (parse-inventory (inventory-url url)))
         (sort-by :reference-time))))

(defn datasource
  "Returns the datasource of `model` at `reference-time`."
  [model reference-time]
  (let [time (to-date-time reference-time)]
    (last (remove #(after? (:reference-time %) time) (datasources model)))))

(defn reference-times
  "Returns the reference times of `model`."
  [model] (map :reference-time (datasources model)))
