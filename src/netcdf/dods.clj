(ns netcdf.dods
  (:import java.util.Calendar java.io.File)
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            clojure.contrib.zip-filter)
  (:use clojure.contrib.str-utils
        clojure.contrib.zip-filter.xml
        incanter.chrono))

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
  (if-let [[_ year month day hour] (re-find #".*(\d{4})(\d{2})(\d{2})/.*(\d{2})z?.*$" uri)]
    (parse-date (str year month day hour) "yyyyMMddHH")))

(defn- feed-to-zip [uri]
  (zip/xml-zip (xml/parse uri)))

(defn parse-dods [xml]
  (xml-> (feed-to-zip xml) :dataset :dods text))

(defn parse-reference-times [xml root]
  (map dataset-url->time (sort (filter #(. % startsWith root) (parse-dods xml)))))

;; (def *local-repository-root* (str (. System getProperty "user.home") File/separator ".weather"))

;; (defn latest-reference-times [repository]
;;   (last (parse-reference-times (inventory-url repository) (:root repository))))

;; (defn latest-reference-time [repository]
;;   (latest-remote-reference-time repository))

;; (defn local-directory [repository valid-time]
;;   (str *local-repository-root*
;;        File/separator
;;        (:name repository)
;;        File/separator
;;        (format-date (valid-time->reference-time valid-time) "yyyyMMdd")))

;; (defn local-filename [repository valid-time variable]
;;   (str (local-directory repository valid-time) File/separator variable ".nc"))

;; (defn local-dataset-url [repository valid-time]
;;   (local-filename repository valid-time))

;; (defn remote-dataset-url [repository valid-time]
;;   (remote-filename repository valid-time))

;; (defn dataset-url [repository valid-time]
;;   (let [local (local-dataset-url repository valid-time)]
;;     (if (file-exists? local) local (remote-dataset-url repository valid-time))))

