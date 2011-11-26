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

(defn forecast-models
  "Returns the models of the forecast."
  [forecast] (set (apply concat (vals (:variables forecast)))))

(defn forecast-variables
  "Returns the variables of the forecast."
  [forecast] (set (keys (:variables forecast))))

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
           model (models-for-variable forecast variable)
           :when (.exists (java.io.File. (variable-path model variable reference-time)))]
       (grid/valid-times (open-grid model variable reference-time)))
     (map set)
     (apply clojure.set/union))))

(defn read-forecast [forecast location & {:keys [reference-time root-dir]}]
  (let [reference-time (or reference-time (latest-reference-time forecast))]
    (if-let [location (resolve-location location)]
      (for [valid-time (valid-times forecast reference-time)]
        (for [[variable models] (:variables forecast)
              :let [model (find-model-by-location models location)]
              :when model]
          (if-let [grid (open-grid model variable reference-time)]
            {:model model
             :location location
             :reference-time reference-time
             :unit (:unit variable)
             :value (interpolate-location grid location :valid-time valid-time)
             :valid-time valid-time
             :variable variable}))))))

(defn read-forecast [forecast location & {:keys [reference-time root-dir]}]
  (let [reference-time (or reference-time (latest-reference-time forecast))]
    (if-let [location (resolve-location location)]
      (for [valid-time (valid-times forecast reference-time)]
        (reduce
         (fn [measure [variable models]]
           (if-let [model (find-model-by-location models location)]
             (if-let [grid (open-grid model variable reference-time)]
               (assoc measure
                 variable
                 {:model model
                  :location location
                  :reference-time reference-time
                  :unit (:unit variable)
                  :value (interpolate-location grid location :valid-time valid-time)
                  :valid-time valid-time})
               measure)
             measure))
         {} (:variables forecast))))))

(defn print-forecast [forecast location & {:keys [reference-time root-dir separator]}]
  (let [separator (or separator "\t")]
    (doseq [measures (read-forecast forecast location :reference-time reference-time :root-dir root-dir)
            :let [measure (first (vals measures))] :when measure]
      (->> [(latitude (:location measure))
            (longitude (:location measure))
            (format-time (:reference-time measure))
            (format-time (:valid-time measure))]
           (concat
            (for [variable (keys measures) :let [measure (get measures variable)]]
              (str (:value measure) separator (:unit measure))))
           (join separator)
           (println)))))

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
  (let [reference-time (first args)]
    (download-forecast surf-forecast :reference-time reference-time)
    (doseq [location (map resolve-location (line-seq (reader *in*))) :when location]
      (print-forecast surf-forecast location :reference-time reference-time))))
