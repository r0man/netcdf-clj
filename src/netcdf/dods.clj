(ns netcdf.dods
  (:refer-clojure :exclude (replace))
  (:import java.util.Calendar java.io.File java.io.File java.net.URI)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [netcdf.dataset :as dataset])
  (:use [clj-time.coerce :only (to-date-time)]
        [clj-time.core :only (after? date-time day hour month year now)]
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

(def parse-inventory
  (memoize
   (fn [url]
     (debug (str "Loading DODS inventory " url " ..."))
     (let [extract (fn [node selector] (first (xml-> node selector text)))]
       (for [dataset (xml-> (feed-to-zip (inventory-url url)) :dataset)]
         {:name (extract dataset :name)
          :description (extract dataset :description)
          :das (extract dataset :das)
          :dds (extract dataset :dds)
          :dods (extract dataset :dods)
          :reference-time (parse-reference-time (extract dataset :dods))})))))

(defn inventory
  "Returns the dataset inventory at `url`."
  [url] (parse-inventory (inventory-url url)))

(defn datasets-by-url
  "Returns all datasets matching the url."
  [url]
  (sort-by :reference-time
           (filter #(and (:dods %) (.startsWith (:dods %) url))
                   (inventory url))))

(defn datasets-by-url-and-reference-time
  "Returns all datasets matching the url and reference time."
  [url reference-time]
  (let [reference-time (to-date-time reference-time)]
    (sort-by :reference-time
             (filter #(and (:dods %) (.startsWith (:dods %) url)
                           (= (:reference-time %) reference-time))
                     (inventory url)))))

(defn reference-times
  "Returns the sorted reference times in the inventory for the model."
  [model] (apply sorted-set (map :reference-time (datasets-by-url (:dods model)))))

(defn reference-time
  "Returns the closest reference time of the model to time."
  [model time]
  (let [time (to-date-time time)]
    (last (remove #(after? % time) (reference-times model)))))

(defn current-reference-time
  "Returns the current reference time of model."
  [model] (reference-time model (now)))

(defn latest-reference-time
  "Returns the latest reference time of model."
  [model] (last (reference-times model)))
