(ns netcdf.dods
  (:import java.util.Calendar java.io.File java.io.File java.net.URI)
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            clojure.contrib.zip-filter
            [netcdf.dataset :as dataset])
  (:use [clojure.contrib.string :only (join replace-re)]
        [clojure.contrib.def :only (defvar)]
        [clj-time.core :only (date-time year month day hour)]
        clojure.contrib.zip-filter.xml
        clj-time.format
        netcdf.utils))

(defvar *cache* (ref {})
  "The inventory cache.")

(defn- feed-to-zip [uri]
  (zip/xml-zip (xml/parse uri)))

(defn inventory-url
  "Returns the url of the xml inventory."
  [url] (replace-re #"/dods.*" "/dods/xml" (str url)))

(defn parse-reference-time [uri]
  (if-let [[_ year month day hour] (re-find #".*(\d{4})(\d{2})(\d{2})/.*(\d{2})z?.*$" (str uri))]
    (try
      (date-time
       (parse-integer year)
       (parse-integer month)
       (parse-integer day)
       (parse-integer hour))
      (catch org.joda.time.IllegalFieldValueException _ nil))))

(defn parse-inventory [url]
  (let [extract (fn [node selector] (first (xml-> node selector text)))]
    (for [dataset (xml-> (feed-to-zip (inventory-url url)) :dataset)]
      {:name (extract dataset :name)
       :description (extract dataset :description)
       :das (extract dataset :das)
       :dds (extract dataset :dds)
       :dods (extract dataset :dods)
       :reference-time (parse-reference-time (extract dataset :dods))})))

(defn find-inventory-by-url
  "Returns the inventory at the url."
  [url]
  (let [inventory-url (inventory-url url)]
    (or (get @*cache* inventory-url)
        (dosync
         (alter *cache* assoc inventory-url (parse-inventory inventory-url))
         (get @*cache* inventory-url)))))

(defn find-datasets-by-url
  "Returns all datasets matching the url."
  [url]
  (sort-by :reference-time
           (filter #(and (:dods %) (.startsWith (:dods %) url))
                   (find-inventory-by-url url))))

(defn find-datasets-by-url-and-reference-time
  "Returns all datasets matching the url and reference time."
  [url reference-time]
  (sort-by :reference-time
           (filter #(and (:dods %) (.startsWith (:dods %) url)
                         (= (:reference-time %) reference-time))
                   (find-inventory-by-url url))))
