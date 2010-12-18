(ns netcdf.model
  (:import java.io.File)
  (:require [netcdf.dods :as dods])
  (:use [clojure.contrib.def :only (defvar)]
        [clj-time.core :only (now in-secs interval date-time year month day hour)]
        [netcdf.dataset :only (copy-dataset open-grid-dataset)]
        netcdf.utils
        netcdf.variable
        clojure.contrib.logging
        clj-time.format))

(defn find-model-by-name
  "Lookup a registered model by name."
  [name] (if-let [var (find-var (symbol (str "netcdf.model/" name)))]
           (deref var)))

(defn reference-times
  "Returns all reference times for model."
  [model] (map :reference-time (dods/find-datasets-by-url (:url model))))

(defn latest-reference-time
  "Returns the latest reference times for model."
  [model] (last (sort (reference-times model))))

(defn local-path [model variable & [reference-time root-dir]]
  (let [reference-time (or reference-time (latest-reference-time model))]
    (str (or root-dir (str (System/getenv "HOME") File/separator ".netcdf")) File/separator
         (:name model) File/separator
         variable File/separator
         (year reference-time) File/separator
         (month reference-time) File/separator
         (day reference-time) File/separator
         (unparse (formatters :basic-time-no-ms) reference-time) ".nc")))

(defn local-uri [model variable & [reference-time root-dir]]
  (java.net.URI. (str "file:" (local-path model variable reference-time root-dir))))

(defn find-dataset [model & [reference-time]]
  (first (dods/find-datasets-by-url-and-reference-time
          (:url model) (or reference-time (latest-reference-time model)))))

(defn copy-variable [model variable filename & [reference-time]]
  (if-let [reference-time (or reference-time (latest-reference-time model))]
    (let [start-time (now) dataset (first (dods/find-datasets-by-url-and-reference-time (:url model) reference-time))]
      (info (str "           Model: " (:name model)))
      (info (str "     Description: " (:description model)))
      (info (str "  Reference Time: " reference-time))
      (info (str "        Variable: " variable))
      (info (str "        Filename: " filename))
      (copy-dataset (:dods dataset) filename [variable])
      (let [duration (interval start-time (now))]
        (info (str "            Size: " (human-file-size filename)))
        (info (str "        Duration: " (human-duration duration)))
        (info (str "   Transfer Rate: " (human-transfer-rate (file-size filename) duration)))
        filename))))

(defn copy-model [model variables & {:keys [reference-time root-dir]}]
  (let [reference-time (or reference-time (latest-reference-time model))]
    (doseq [variable variables :let [filename (local-path model variable reference-time root-dir)]]
      (copy-variable model variable filename reference-time))))

(defn global-forecast-system-models
  "Returns the Wave Watch III models."
  [] (remove nil? (map find-model-by-name ["gfs-hd"])))

(defn wave-watch-models
  "Returns the Wave Watch III models."
  [] (remove nil? (map find-model-by-name ["akw" "enp" "nah" "nph" "nww3" "wna"])))

(defn download-global-forecast-system [& [reference-time variables]]
  (info "Downloading Global Forecast System ...")
  (doall (map #(copy-model % (or variables ["tmpsfc"]) :reference-time reference-time)
              (global-forecast-system-models))))

(defn download-wave-watch [& [reference-time variables]]
  (info "Downloading Wave Watch III ...")
  (doall (map #(copy-model % (or variables ["htsgwsfc"]) :reference-time reference-time)
              (wave-watch-models))))

(defn meta-data [model reference-time]
  (if-let [dataset (first (dods/find-datasets-by-url-and-reference-time (:url model) reference-time))]
    (with-open [netcdf (open-grid-dataset (:dods dataset))]
      (assoc dataset
        :name (:name model)
        :bounds (.getBoundingBox netcdf)
        :description (:description model)))))

(defmacro defmodel
  "Define and register the model."
  [name description & attributes]
  (let [name# name]
    `(def ~name# ~(assoc (apply hash-map attributes) :name (str name#) :description description))))

;; (latest-reference-time nww3)
;; (download-wave-watch)
;; (download-global-forecast-system)

(defmodel akw
  "Regional Alaska Waters Wave Model"
  :url "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
  :variables wave-watch-variables)

(defmodel enp
  "Regional Eastern North Pacific Wave Model"
  :url "http://nomads.ncep.noaa.gov:9090/dods/wave/enp"
  :variables wave-watch-variables)

(defmodel nah
  "Regional Atlantic Hurricane Wave Model"
  :url "http://nomads.ncep.noaa.gov:9090/dods/wave/nah"
  :variables wave-watch-variables)

(defmodel nph
  "Regional North Pacific Hurricane Wave Model"
  :url "http://nomads.ncep.noaa.gov:9090/dods/wave/nph"
  :variables wave-watch-variables)

(defmodel nww3
  "Global NOAA Wave Watch III Model"
  :url "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"
  :variables wave-watch-variables)

(defmodel wna
  "Regional Western North Atlantic Wave Model"
  :url "http://nomads.ncep.noaa.gov:9090/dods/wave/wna"
  :variables wave-watch-variables)

(defmodel gfs-hd
  "Global Forecast Model"
  :url "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  :variables gfs-variables)
