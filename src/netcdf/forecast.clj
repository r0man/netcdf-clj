(ns netcdf.forecast
  (:gen-class)
  (:require [netcdf.dods :as dods])
  (:use [netcdf.geo-grid :only (interpolate-location open-geo-grid valid-times)]
        [netcdf.location :only (parse-location)]
        [netcdf.time :only (format-time)]
        [clojure.java.io :only (reader)]
        [clojure.string :only (join)]
        netcdf.model
        netcdf.location
        netcdf.repository
        netcdf.variable
        [clojure.data.json :ony (read-json)]))

(def ^:dynamic *cache* (atom {}))

(defrecord Forecast [name description variables])

(defn make-forecast
  "Make a new forecast."
  [& {:as options}] (map->Forecast options))

(defmacro defforecast
  "Define a forecast."
  [name description & variables]
  (let [name# name variables# (apply hash-map variables)]
    `(def ~name#
       (make-forecast
        :name ~(str name#)
        :description ~description
        :variables ~variables#))))

(defn variables [forecast]
  (set (keys (:variables forecast))))

(defn models-for-variable [forecast variable]
  (get (:variables forecast) variable))

(defn download-forecast [forecast & {:keys [directory reference-time]}]
  (doseq [variable (keys (:variables forecast))
          model (get (:variables forecast) variable)]
    (download-variable model variable :reference-time reference-time :root-dir directory)))

(defn open-grid
  "Lookup the geo grid in *cache*, or open it."
  [model variable reference-time]
  (let [path (variable-path model variable reference-time)]
    (or (get @*cache* path)
        (let [grid (open-geo-grid path (:name variable))]
          (swap! *cache* assoc path grid)
          grid))))

(defn read-forecast [forecast location & {:keys [reference-time]}]
  (if-let [location (resolve-location location)]
    (flatten
     (for [variable (keys (:variables forecast))]
       (let [model (find-model-by-location (models-for-variable forecast variable) location)
             grid (open-grid model variable (or reference-time (dods/latest-reference-time model)))]
         (for [valid-time (valid-times grid)]
           {:model model
            :location location
            :reference-time reference-time
            :unit (:unit variable)
            :value (interpolate-location grid location :valid-time valid-time)
            :valid-time valid-time
            :variable variable}))))))

(defn to-csv [measure & {:keys [separator]}]
  (join
   (or separator "\t")
   [(or (:id (:model measure)) (:name (:model measure)))
    (or (:id (:variable measure)) (:name (:variable measure)))
    (format-time (:reference-time measure))
    (format-time (:valid-time measure))
    (latitude (:location measure))
    (longitude (:location measure))
    (:value measure)
    (:unit measure)]))

(defn print-forecast [forecast location & {:keys [reference-time separator]}]
  (doseq [measure (read-forecast forecast "mundaka" :reference-time reference-time)]
    (println (to-csv measure :separator separator))))

(defn stdin-location-seq []
  (remove nil? (map resolve-location (line-seq (reader *in*)))))

(defforecast surf-forecast
  "The surf forecast."
  dirpwsfc wave-watch-models
  dirswsfc wave-watch-models
  htsgwsfc wave-watch-models
  perpwsfc wave-watch-models
  perswsfc wave-watch-models
  ugrdsfc wave-watch-models
  vgrdsfc wave-watch-models
  wdirsfc wave-watch-models
  windsfc wave-watch-models
  wvdirsfc wave-watch-models
  wvpersfc wave-watch-models
  tmpsfc global-forecast-system-models
  tcdcclm global-forecast-system-models)

(defn -main [& args]
  (doseq [location (stdin-location-seq)]
    (print-forecast surf-forecast location)))

;; "2011-09-24T12:00:00Z"