(ns netcdf.model
  (:require [netcdf.dods :as dods])
  (:use [clojure.contrib.def :only (defvar)]))

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

(defn global-forecast-system-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map find-model-by-name ["gfs-hd"])))

(defn wave-watch-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map find-model-by-name ["akw" "enp" "nah" "nph" "nww3" "wna"])))

;; (defn download-model [model & variables]  
;;   (doseq [model repositories]
;;     (println (str "* " (:description model)))
;;     (println (str "  Url: " (:url model)))
;;     (doseq [variable variables]
;;       (time
;;        (do
;;          (print (str "  - " variable " "))
;;          (throw (Exception. "FIXME"))
;;          ;; (println (str (download-variable model variable)))
;;          (print "    "))))))

;; (defn download-global-forecast-system [& variables]
;;   (println "Downloading the Global Forecast System Model ...")
;;   (download-variables (global-forecast-system-repositories) (or variables ["tmpsfc"])))

;; (defn download-wave-watch [& variables]
;;   (println "Downloading the Wave Watch III Model ...")
;;   (download-variables (wave-watch-repositories) (or variables ["htsgwsfc"])))

(defn reference-times
  "Returns all reference times for model."
  [model] (map :reference-time (dods/find-datasets-by-url (:url model))))

(defn latest-reference-time
  "Returns the latest reference times for model."
  [model] (last (sort (reference-times model))))

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
