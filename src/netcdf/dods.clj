(ns netcdf.dods
  (:import java.util.Calendar java.io.File)
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            clojure.contrib.zip-filter)
  (:use clojure.contrib.str-utils
        clojure.contrib.zip-filter.xml
        incanter.chrono))

(def *local-repository-root* (str (. System getProperty "user.home") File/separator ".weather"))

(defstruct repository :name :root :description)

(defn valid-time->reference-time [valid-time]
  (date
   (valid-time :year)
   (valid-time :month)
   (valid-time :day)
   (* (int (/ (valid-time :hour) 6)) 6)
   0))

(defn uri->time [uri]
  (if-let [[_ year month day hour] (re-find #".*(\d{4})(\d{2})(\d{2})/.*(\d{2})z?.*$" uri)]
    (parse-date (str year month day hour) "yyyyMMddHH")))

(defn make-repository [name root & [description]]
  (struct repository name root description))

(defn server-directory [repository]
  (str (:root repository) "/xml"))

(defn dataset-directory [repository valid-time]
  (str (:name repository) (format-date (valid-time->reference-time valid-time) "yyyyMMdd")))

(defn dataset-filename [repository valid-time]
  (str (:name repository) "_" (format-date (valid-time->reference-time valid-time) "HH") "z"))

(defn dataset-uri [repository valid-time]
  (str (:root repository) "/" (dataset-directory repository valid-time) "/" (dataset-filename repository valid-time)))

(defn- feed-to-zip [uri]
  (zip/xml-zip (xml/parse uri)))

(defn parse-dods [xml]
  (xml-> (feed-to-zip xml) :dataset :dods text))

(defn parse-reference-times [xml root]
  (map uri->time (sort (filter #(. % startsWith root) (parse-dods xml)))))

;; (defn latest-reference-times [repository]
;;   (last (parse-reference-times (server-directory repository) (:root repository))))

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

;; (defn local-dataset-uri [repository valid-time]
;;   (local-filename repository valid-time))

;; (defn remote-dataset-uri [repository valid-time]
;;   (remote-filename repository valid-time))

;; (defn dataset-uri [repository valid-time]
;;   (let [local (local-dataset-uri repository valid-time)]
;;     (if (file-exists? local) local (remote-dataset-uri repository valid-time))))

