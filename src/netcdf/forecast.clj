(ns netcdf.forecast
  (:gen-class)
  (:require [netcdf.geo-grid :as grid]
            [netcdf.model :as model]
            [netcdf.variable :as variable])
  (:use [clojure.data.json :ony (read-json)]
        [clojure.java.io :only (reader)]
        [clojure.string :only (join)]
        [netcdf.location :only (parse-location)]
        [netcdf.model :only (find-model-by-location global-forecast-system-models wave-watch-models)]
        [netcdf.time :only (format-time)]
        [netcdf.variable :exclude (valid-times)]
        [netcdf.geo-grid :only (interpolate-location open-geo-grid)]
        netcdf.location
        netcdf.repository))

(def ^:dynamic *cache* (atom {}))

(defrecord Forecast [name description variables])

(def cached-open-gird (memoize open-grid))

(defn make-forecast
  "Make a new forecast."
  [& {:as options}] (map->Forecast options))

(defmacro defforecast
  "Define a forecast."
  [name description & variables]
  (let [name# name variables# (apply sorted-map variables)]
    `(def ~name#
       (make-forecast
        :name ~(str name#)
        :description ~description
        :variables ~variables#))))

(defn forecast-models
  "Returns the models of the forecast."
  [forecast] (set (apply concat (vals (:variables forecast)))))

(defn forecast-variables
  "Returns the variables of the forecast."
  [forecast] (set (keys (:variables forecast))))

(defn variable-keywords
  "Returns the variable names of forecast as keywords."
  [forecast ] (map (comp keyword :name) (forecast-variables forecast)))

(defn average-weather
  "Calculates the average weather."
  [forecast weather-seq]
  (let [variables (variable-keywords forecast)]
    (for [[valid-time weather] (group-by :valid-time weather-seq)]
      (reduce
       (fn [avg-weather variable]
         (let [values (remove #(or (nil? %1) (Double/isNaN %1)) (map variable weather))]
           (if (empty? values)
             avg-weather
             (assoc avg-weather variable (/ (reduce + values) (count values))))))
       {:valid-time valid-time} variables))))

(defn models-for-variable [forecast variable]
  (get (:variables forecast) variable))

(defn download-forecast [forecast & {:keys [directory reference-time]}]
  (doseq [variable (keys (:variables forecast))
          model (get (:variables forecast) variable)]
    (download-variable model variable :reference-time reference-time :root-dir directory)))

(defn latest-reference-time
  "Returns the latest reference-time of the models in the forecast."
  [forecast]
  (->> (forecast-models forecast)
       (map model/latest-reference-time )
       (apply sorted-set)
       (last)))

(defn valid-times
  "Returns the valid times of the forecast."
  [forecast & [reference-time]]
  (let [reference-time (or reference-time (latest-reference-time forecast))]
    (->>
     (for [variable (keys (:variables forecast))
           model (models-for-variable forecast variable)]
       (grid/valid-times (cached-open-gird model variable reference-time)))
     (map set)
     (apply clojure.set/union))))

(defn read-forecast [forecast location & {:keys [reference-time]}]
  (let [reference-time (or reference-time (latest-reference-time forecast))]
    (if-let [location (resolve-location location)]
      (for [valid-time (valid-times forecast reference-time)]
        (reduce
         (fn [measure [variable models]]
           (if-let [model (find-model-by-location models location)]
             (if-let [grid (cached-open-gird model variable reference-time)]
               (assoc measure
                 (keyword (:name variable)) (interpolate-location grid location :valid-time valid-time)
                 :location location
                 :reference-time reference-time
                 :valid-time valid-time)
               measure)
             measure))
         {} (:variables forecast))))))

(defn dump-forecast
  "Dump the forecast at location to standard out."
  [forecast location & {:keys [reference-time]}]
  (let [variables (sort-by :name (keys (:variables forecast)))]
    (doseq [forecast (read-forecast forecast location :reference-time reference-time)
            :let [location (:location forecast)]]
      (->>  (map #(get forecast (keyword (:name %1))) variables)
            (concat [(latitude location) (longitude location)] [])
            (join "\t")
            (println)))))

(defforecast wave-watch-3
  "The NOAA Wave Watch III forecast."
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
  wvpersfc wave-watch-models)

(defforecast surf-forecast
  "The Burningswell surf forecast."
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
