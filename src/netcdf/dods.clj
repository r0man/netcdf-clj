(ns netcdf.dods
  (:refer-clojure :exclude (replace))
  (:import java.util.Calendar java.io.File java.io.File java.net.URI)
  (:require [clojure.core.memoize :refer [memo-ttl]]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [netcdf.dataset :as dataset])
  (:use [clj-time.coerce :only (to-date-time)]
        [clj-time.core :only (after? date-time day hour month year now )]
        [clojure.string :only (join replace)]
        [clojure.data.zip.xml :only (xml-> text)]
        clj-time.format
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

(defn parse-inventory* [url]
  (debug (str "Parsing DODS inventory " url " ..."))
  (let [extract (fn [node selector] (first (xml-> node selector text)))]
    (for [dataset (xml-> (feed-to-zip (inventory-url url)) :dataset)]
      {:name (extract dataset :name)
       :description (extract dataset :description)
       :das (extract dataset :das)
       :dds (extract dataset :dds)
       :dods (extract dataset :dods)
       :reference-time (parse-reference-time (extract dataset :dods))})))

(def parse-inventory
  ;; Cache inventories for 15 minutes.
  (memo-ttl parse-inventory* (* 15 60 1000)))

(defn datasources
  "Returns the datasources of `model`."
  [model]
  (let [url (:dods model)]
    (->> (filter #(and (:dods %) (.startsWith (:dods %) url))
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
