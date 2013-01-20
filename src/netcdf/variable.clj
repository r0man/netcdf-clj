(ns netcdf.variable
  (:import java.io.File)
  (:require [netcdf.dods :as dods]
            [netcdf.geo-grid :as grid])
  (:use [clj-time.coerce :only (to-date-time)]
        [clj-time.core :only (now interval)]
        [clojure.string :only (join)]
        [netcdf.dataset :only (copy-dataset find-geo-grid open-grid-dataset)]
        clj-time.format
        clojure.tools.logging
        netcdf.repository
        netcdf.utils
        netcdf.time))

(def ^:dynamic *variables* (atom {}))

(defrecord Variable [name description unit])

(defn make-variable
  "Make a NetCDF variable."
  [& {:keys [name description unit]}]
  (Variable. name description unit))

(defn variable
  "Returns the variable by name."
  [name] (get @*variables* (keyword name)))

(defn variable?
  "Returns true if arg is a variable, otherwise false."
  [arg] (instance? Variable arg))

(defmacro defvariable
  "Define a NetCDF variable."
  [name description & {:keys [unit]}]
  (let [name# name description# description unit# unit]
    `(do
       (def ~name#
         (make-variable
          :name ~(str name#)
          :description ~description#
          :unit ~unit#))
       (swap! *variables* assoc (keyword (:name ~name#)) ~name#))))

(defn download-variable [model variable & {:keys [reference-time root-dir]}]
  (if-let [reference-time (to-date-time (or reference-time (last (dods/reference-times model))))]
    (let [start-time (now)
          source (dods-dataset-url model variable reference-time)
          target (local-dataset-url model variable reference-time root-dir)]
      (infof "Downloading %s model %s ..." (:description variable) (:description model))
      (infof "  Model........... %s (%s)" (:description model) (:name model))
      (infof "  Variable........ %s (%s)" (:description variable) (:name variable))
      (infof "  Reference Time.. %s" (unparse (formatters :rfc822) reference-time))
      (infof "  DODS Url........ %s" source)
      (infof "  File Name....... %s" target)
      (if-not (> (file-size target) 0)
        (let [_ (copy-dataset source target [(:name variable)])
              interval (interval start-time (now))]
          (infof "  File Size....... %s" (human-file-size target))
          (infof "  Transfer Rate... %s" (human-transfer-rate (file-size target) interval))
          (infof "  Duration........ %s" (human-duration interval)))
        (infof "  File Size....... %s" (human-file-size target)))
      (assoc variable
        :interval (interval start-time (now))
        :filename target
        :reference-time reference-time
        :size (file-size target)))))

(defn read-variable [model variable pois & [reference-time]]
  (if-let [reference-time (to-date-time (or reference-time (last (reference-times model))))]
    (with-open [dataset (open-grid model variable reference-time)]
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

(defn valid-times
  "Returns the valid times of the variable."
  [model variable & [reference-time]]
  (let [reference-time (or reference-time (last (reference-times model)))]
    (with-open [dataset (open-grid-dataset (dataset-url model variable reference-time))]
      (grid/valid-times (find-geo-grid dataset (:name variable))))))

(defn variable-fragment
  "Returns the variable fragment."
  [model variable reference-time]
  (->> [(:name model)
        (:name variable)
        (str (date-time-path-fragment reference-time) ".nc")]
       (join File/separator)))

(defvariable dirpwsfc
  "Primary wave direction"
  :unit "°")

(defvariable dirswsfc
  "Secondary wave direction"
  :unit "°")

(defvariable htsgwsfc
  "Significant height of combined wind waves and swell"
  :unit "m")

(defvariable perpwsfc
  "Primary wave mean period"
  :unit "s")

(defvariable perswsfc
  "Secondary wave mean period"
  :unit "s")

(defvariable tcdcclm
  "Total cloud cover"
  :unit "s")

(defvariable tmpsfc
  "Surface temperature"
  :unit "k")

(defvariable ugrdsfc
  "U-component of wind"
  :unit "m/s")

(defvariable vgrdsfc
  "V-component of wind"
  :unit "m/s")

(defvariable wdirsfc
  "Wind direction"
  :unit "°")

(defvariable windsfc
  "Wind speed"
  :unit "m/s")

(defvariable wvdirsfc
  "Direction of wind waves"
  :unit "°")

(defvariable wvpersfc
  "Mean period of wind waves"
  :unit "s")

(def global-forecast-system-variables
  [tmpsfc
   tcdcclm])

(def wave-watch-variables
  [dirpwsfc
   dirswsfc
   htsgwsfc
   perpwsfc
   perswsfc
   ugrdsfc
   vgrdsfc
   wdirsfc
   windsfc
   wvdirsfc
   wvpersfc])
