(ns netcdf.model
  (:require [netcdf.dods :as dods])
  (:use netcdf.bounding-box
        netcdf.location))

(def ^:dynamic *models* (atom {}))

(defrecord Model [name description bounding-box dods resolution])

(defn make-model [& {:keys [name description bounding-box dods resolution]}]
  (Model. name description bounding-box dods resolution))

(defn model?
  "Returns true if arg is a model, otherwise false."
  [arg] (isa? (class arg) Model))

(defn model
  "Returns the model by name."
  [name] (get @*models* (keyword name)))

(defn register-model
  "Register a model by name."
  [model] (swap! *models* assoc (keyword (:name model)) model))

(defmacro defmodel
  "Define and register the model."
  [name description & {:keys [bounding-box dods resolution]}]
  (let [name# name description# description bounding-box# bounding-box dods# dods resolution# resolution]
    `(do (def ~name#
           (make-model
            :bounding-box ~bounding-box#
            :description ~description#
            :dods ~dods#
            :name ~(str name#)
            :resolution ~resolution#))
         (register-model ~name#))))

(defn sort-by-resolution
  "Sort the models by their resolution."
  [models] (sort-by #(* (:latitude (:resolution %)) (:longitude (:resolution %))) models))

(defn find-model-by-location
  "Returns the model with the highest resolution that covers the location."
  [models location] (first (filter #(contains-location? (:bounding-box %) location) (sort-by-resolution models))))

(defn reference-times
  "Returns the reference times of model."
  [model] (dods/reference-times model))

(defn latest-reference-time
  "Returns the latest reference time of model."
  [model] (last (reference-times model)))

(defmodel akw
  "Regional Alaska Waters Wave Model"
  :bounding-box (make-bounding-box 44.75 159.5 75.25 -123.5)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
  :resolution {:latitude 0.25 :longitude 0.5})

(defmodel enp
  "Regional Eastern North Pacific Wave Model"
  :bounding-box (make-bounding-box 4.75 -170.25 60.5 -77.25)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/enp"
  :resolution {:latitude 0.25 :longitude 0.25})

(defmodel gfs-hd
  "Global Forecast Model"
  :bounding-box (make-bounding-box -90.0 0.0 90.0 -0.5)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  :resolution {:latitude 0.5 :longitude 0.5})

(defmodel nah
  "Regional Atlantic Hurricane Wave Model"
  :bounding-box (make-bounding-box -0.25 -98.25 50.25 -29.75)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nah"
  :resolution {:latitude 0.5 :longitude 0.25})

(defmodel nph
  "Regional North Pacific Hurricane Wave Model"
  :bounding-box (make-bounding-box 4.75 -170.25 60.5 -77.25)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nph"
  :resolution {:latitude 0.25 :longitude 0.5})

(defmodel nww3
  "Global NOAA Wave Watch III Model"
  :bounding-box (make-bounding-box -78.0 0.0 78.0 -1.25)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"
  :resolution {:latitude 1.0 :longitude 1.25})

(defmodel wna
  "Regional Western North Atlantic Wave Model"
  :bounding-box (make-bounding-box -0.25 -98.25 50.25 -29.75)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/wna"
  :resolution {:latitude 0.5 :longitude 0.5})

(def global-forecast-system-models
  [gfs-hd])

(def wave-watch-models
  [nww3 akw enp nah nph wna])
