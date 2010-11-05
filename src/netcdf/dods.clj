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
  "Returns the parsed inventory for the url." 
  [url]
  (let [inventory-url (inventory-url url)]
    (or (get @*cache* inventory-url)
        (dosync
         (alter *cache* assoc inventory-url (parse-inventory inventory-url))
         (get @*cache* inventory-url)))))

(defn find-datasets-by-url
  "Returns all datasets whose dods url matches the given url."
  [url] (filter #(and (:dods %) (.startsWith (:dods %) url))
                (find-inventory-by-url url)))

(defn find-datasets-by-url-and-reference-time
  [url reference-time]
  (filter #(and (:dods %)
                (.startsWith (:dods %) url)
                (= (:reference-time %) reference-time))
          (find-inventory-by-url url)))

;; (count (find-dataset-by-url-and-reference-times-by-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"))

;; (count (inventory "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"))
;; (inventory "file:/home/roman/workspace/netcdf-clj/test-resources/dods/xml")

;; (def *local-url* (str (System/getProperty "user.home") File/separator ".netcdf"))



;; (defn valid-time->reference-time [valid-time]
;;   (date-time
;;    (year valid-time)
;;    (month valid-time)
;;    (day valid-time)
;;    (* (int (/ (hour valid-time) 6)) 6)
;;    0))

;; (defn dataset-directory [repository valid-time]
;;   (str (:name repository) (unparse (formatters :basic-date) (valid-time->reference-time valid-time))))

;; (defn dataset-filename [repository valid-time]
;;   (str (:name repository) "_" (unparse (formatters :hour) (valid-time->reference-time valid-time)) "z"))

;; (defn dataset-url [repository valid-time]
;;   (str (:url repository) "/" (dataset-directory repository valid-time) "/" (dataset-filename repository valid-time)))

;; (defn parse-dods [xml]
;;   (xml-> (feed-to-zip xml) :dataset :dods text))

;; (defn parse-reference-times [xml url]
;;   (map parse-reference-time (sort (filter #(. % startsWith url) (parse-dods xml)))))



;; (defn reference-times [repository]
;;   (parse-reference-times (inventory-url repository) (:url repository)))

;; (defn latest-reference-time [repository]
;;   (last (reference-times repository)))

;; (defn local-url [repository valid-time & [variable]]
;;   (let [reference-time (valid-time->reference-time valid-time)]
;;     (URI. (str "file:" *local-url* File/separator (:name repository) File/separator
;;                (if variable (str variable File/separator))
;;                (unparse (formatters :basic-date) reference-time) File/separator "t"
;;                (unparse (formatters :hour) reference-time) "z.nc"))))

;; (defn download-variable [repository variable & [reference-time]]
;;   (let [reference-time (or reference-time (latest-reference-time repository))
;;         remote (dataset-url repository reference-time)
;;         local (local-url repository reference-time variable)]
;;     (if-not (file-exists? local)
;;       (dataset/copy-dataset remote local [variable]))
;;     local))

;; (defn find-dataset []

;;   )

;; (inventory-url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3")

;; ;; (find-datasets "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3")

;; ;; ;; (defserver nomads
;; ;; ;;   "NOAA Operational Model Archive and Distribution System"
;; ;; ;;   :hostname "nomads.ncep.noaa.gov"
;; ;; ;;   :port 9090)

;; ;; (parse-inventory "test-resources/dods/xml")

;; ;; (def uri (java.net.URI. "http://nomads.ncep.noaa.gov:9090/dods/xml"))



;; ;; (def das (.getDAS (dods.dap.DConnect. "http://nomads.ncep.noaa.gov:9090/dods/fnl/fnl20101030/fnlflx_00z")))

;; ;; (.print das System/out)
;; ;; (enumeration-seq (.getNames das))

;; ;; (.print (.getAttributeTable das "tmax2m") System/out)
