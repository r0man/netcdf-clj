(ns netcdf.model
  (:import java.io.File)
  (:require [netcdf.dods :as dods])
  (:use [clojure.contrib.def :only (defvar)]
        [clj-time.core :only (date-time year month day hour)]
        [netcdf.dataset :only (copy-dataset)]
        clj-time.format))

(defvar *repositories* (ref {})
  "The map of repositories.")

(defrecord Model [name url description])

(defn find-model-by-name
  "Lookup a registered model by name."
  [name] (get @*repositories* (keyword name)))

(defn make-model [name url & [description]]
  (Model. name url description))

(defn register-model
  "Register the model."
  [model]
  (dosync (ref-set *repositories* (assoc @*repositories* (keyword (:name model)) model)))
  model)

(defn model?
  "Returns true if arg is a model, otherwise false."
  [arg] (isa? (class arg) Model))

(defn unregister-model
  "Unregister the model."
  [model]
  (dosync (ref-set *repositories* (dissoc @*repositories* (keyword (:name model)))))
  model)

(defn reference-times
  "Returns all reference times for model."
  [model] (map :reference-time (dods/find-datasets-by-url (:url model))))

(defn latest-reference-time
  "Returns the latest reference times for model."
  [model] (last (sort (reference-times model))))

(defn local-path [model variable & [reference-time root-dir]]
  (let [reference-time (or reference-time (latest-reference-time model))]
    (str (or root-dir ".") File/separator
         (:name model) File/separator variable File/separator
         (unparse (formatters :basic-date) reference-time) File/separator
         (unparse (formatters :hour) reference-time) ".nc")))

(defn find-dataset [model & [reference-time]]
  (first (dods/find-datasets-by-url-and-reference-time
          (:url model) (or reference-time (latest-reference-time model)))))

(defn copy-variable [model variable filename & [reference-time]]
  (if-let [reference-time (or reference-time (latest-reference-time model))]    
    (let [dataset (first (dods/find-datasets-by-url-and-reference-time (:url model) reference-time))]
      (copy-dataset (:dods dataset) filename [variable]))))

(defn copy-model [model variables & {:keys [reference-time root-dir]}]  
  (doseq [variable variables :let [filename (local-path model variable reference-time root-dir)]]
    (copy-variable model variable filename reference-time)))

(defn global-forecast-system-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map find-model-by-name ["gfs-hd"])))

(defn wave-watch-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map find-model-by-name ["akw" "enp" "nah" "nph" "nww3" "wna"])))

(defn download-global-forecast-system [& variables]
  (println "Downloading the Global Forecast System Model ...")
  (map #(copy-model (or variables ["tmpsfc"])) (global-forecast-system-repositories)))

(defn download-wave-watch [& variables]
  (println "Downloading the Wave Watch III Model ...")
  (map #(copy-model % (or variables ["htsgwsfc"])) (wave-watch-repositories)))

;; (download-wave-watch)

(defmacro defmodel
  "Define and register the model."
  [name url & [description]]
  (register-model (make-model name url description)))

(defmodel "akw"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
  "Regional Alaska Waters Wave Model")

(defmodel "enp"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/enp"
  "Regional Eastern North Pacific Wave Model")

(defmodel "nah"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/nah"
  "Regional Atlantic Hurricane Wave Model")

(defmodel "nph"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/nph"
  "Regional North Pacific Hurricane Wave Model")

(defmodel "nww3"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"
  "Global NOAA Wave Watch III Model")

(defmodel "wna"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/wna"
  "Regional Western North Atlantic Wave Model")

(defmodel "gfs-hd"
  "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  "Global Forecast Model")
