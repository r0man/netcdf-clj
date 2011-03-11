(ns netcdf.model
  (:import java.io.File org.joda.time.Interval)
  (:require [netcdf.dods :as dods]
            [netcdf.geo-grid :as grid])
  (:use [clojure.contrib.def :only (defvar)]
        [clojure.string :only (join)]
        [clojure.contrib.duck-streams :only (with-out-writer)]
        [clojure.contrib.json :only (read-json json-str)]
        [clj-time.core :only (now in-secs interval date-time year month day hour)]
        [netcdf.dataset :only (copy-dataset find-geo-grid open-grid-dataset)]
        netcdf.bounding-box
        netcdf.utils
        netcdf.variable
        netcdf.time
        clojure.contrib.logging
        clj-time.format))

(defrecord Model [name description dods variables])

(defvar *cache* (ref {})
  "The model cache.")

(defvar *root-dir*
  (str (System/getenv "HOME") File/separator ".netcdf")
  "The local NetCDF directory.")

(defn make-model [& {:keys [name description dods variables]}]
  (Model. name description dods variables))

(defn register-model [model]
  (dosync (ref-set *cache* (assoc @*cache* (keyword (:name model)) model))))

(defmacro defmodel
  "Define and register the model."
  [name description & {:keys [dods variables]}]
  (let [name# name description# description dods# dods variables# variables]
    `(do (defvar ~name#
           (make-model
            :description ~description#
            :name ~(str name#)
            :dods ~dods#
            :variables ~variables#))
         (register-model ~name#))))

(defn model [name]
  (get @*cache* name))

(defn reference-times
  "Returns all reference times for model."
  [model] (map :reference-time (dods/find-datasets-by-url (:dods model))))

(defn latest-reference-time
  "Returns the latest reference times for model."
  [model] (last (sort (reference-times model))))

(defn variable-path [model variable & [reference-time root-dir]]
  (let [reference-time (or reference-time (latest-reference-time model))]
    (join File/separator
          [(or root-dir *root-dir*) (:name model) (:name variable)
           (str (date-time-path-fragment reference-time) ".nc")])))

(defn local-uri [model variable & [reference-time root-dir]]
  (java.net.URI. (str "file:" (variable-path model variable reference-time root-dir))))

(defn find-dataset [model & [reference-time]]
  (first (dods/find-datasets-by-url-and-reference-time
          (:dods model) (or reference-time (latest-reference-time model)))))

(defn download-variable [model variable & {:keys [reference-time root-dir]}]
  (if-let [reference-time (or reference-time (latest-reference-time model))]
    (let [start-time (now)
          dataset (first (dods/find-datasets-by-url-and-reference-time (:dods model) reference-time))
          filename (variable-path model variable reference-time root-dir)]
      (info (str "           Model: " (:description model) " (" (:name model) ")"))
      (info (str "  Reference Time: " (unparse (formatters :rfc822) reference-time)))
      (info (str "        Variable: " (:description variable) " (" (:name variable) ")"))
      (info (str "     NetCDF File: " filename))
      (if-not (> (file-size filename) 0)
        (let [dataset (copy-dataset (:dods dataset) filename [(:name variable)])
              interval (interval start-time (now))]
          (info (str "            Size: " (human-file-size filename)))
          (info (str "   Transfer Rate: " (human-transfer-rate (file-size filename) interval)))
          (info (str "        Duration: " (human-duration interval))))
        (info (str "            Size: " (human-file-size filename))))
      (assoc variable
        :interval (interval start-time (now))
        :filename filename
        :reference-time reference-time
        :size (file-size filename)))))

(defn download-model [model & options]
  (let [variables (remove nil? (map #(apply download-variable model % options) (:variables model)))]
    (assoc model
      :interval (Interval.
                 (.getStartMillis (:interval (first variables)))
                 (.getEndMillis (:interval (last variables))))
      :reference-time (:reference-time (first variables))
      :size (reduce + (remove nil? (map :size variables)))
      :variables variables)))

(defn download-models [models & [reference-time]]
  (doall (map #(download-model % :reference-time reference-time) models)))

(defn meta-data [model reference-time]
  (if-let [dataset (first (dods/find-datasets-by-url-and-reference-time (:dods model) reference-time))]
    (with-open [netcdf (open-grid-dataset (:dods dataset))]
      (assoc dataset
        :name (:name model)
        :bounds (.getBoundingBox netcdf)
        :description (:description model)))))

(defn model-info [model]
  (let [latest-reference-time (latest-reference-time model)]
    (assoc model :bounding-box (to-map (:bounds (meta-data model latest-reference-time))))))

(defn write-model-info [filename models]
  (spit filename (prn-str (map model-info models))))

(defn read-model-info [filename]
  (read-string (slurp filename)))

(defn read-variable [model variable pois & [reference-time]]
  (let [reference-time (or reference-time (latest-reference-time model))
        filename (variable-path model variable reference-time)]
    (if-not (file-exists? filename)
      (download-variable model variable :reference-time reference-time))
    (with-open [dataset (open-grid-dataset filename)]
      (let [grid (find-geo-grid dataset (:name variable))]
        (doall
         (for [valid-time (grid/valid-times grid)
               poi (if (sequential? pois) pois [pois])
               :let [value (grid/read-location grid (:location poi) :valid-time valid-time)]
               :when (not (Double/isNaN value))]
           {:location (:location poi)
            :id (:id poi)
            :reference-time reference-time
            :valid-time valid-time
            :value value
            :variable variable}))))))

(defn read-model [model location & [reference-time]]
  (let [reference-time (or reference-time (latest-reference-time model))]
    (flatten (map #(read-variable model % location reference-time) (:variables model)))))

(defn dump-model [model filename pois & [reference-time separator]]
  (with-out-writer filename
    (doseq [meassure (read-model model pois reference-time)]
      (println
       (join
        (or separator "\t")
        [(:id model)
         (:id (:variable meassure))
         (:id meassure)
         (:latitude (:location meassure))
         (:longitude (:location meassure))
         (format-time (:reference-time meassure))
         (format-time (:valid-time meassure))
         (:value meassure)
         (:unit (:variable meassure))])))))

(defmodel akw
  "Regional Alaska Waters Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
  :variables wave-watch-variables)

(defmodel enp
  "Regional Eastern North Pacific Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/enp"
  :variables wave-watch-variables)

(defmodel gfs-hd
  "Global Forecast Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  :variables gfs-variables)

(defmodel nah
  "Regional Atlantic Hurricane Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nah"
  :variables wave-watch-variables)

(defmodel nph
  "Regional North Pacific Hurricane Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nph"
  :variables wave-watch-variables)

(defmodel nww3
  "Global NOAA Wave Watch III Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"
  :variables wave-watch-variables)

(defmodel wna
  "Regional Western North Atlantic Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/wna"
  :variables wave-watch-variables)

(defvar gfs-models
  [gfs-hd] "The models of the Global Forecast System.")

(defvar wave-watch-models
  [nww3 akw enp nah nph wna]
  "The the Wave Watch III models.")

(defn download-gfs [& [reference-time]]
  (download-models gfs-models reference-time))

(defn download-wave-watch [& [reference-time]]
  (download-models wave-watch-models reference-time))

;; (defn read-spots [filename]
;;   (read-json (slurp filename)))

;; (def *spots* (read-spots "/home/roman/spots.json"))

;; (read-variable nww3 (first (:variables nww3)) (take 10 *spots*))

;; (take 2 *locations*)

;; (def *pois* [{:location {:latitude 0 :longitude 0}}])

;; (time
;;  (dump-model nww3 "/tmp/nww3.csv" *pois*))

;; (take 2 (read-model nww3 [{:latitude 0 :longitude 0} {:latitude 1 :longitude 1}]))
