(ns netcdf.variable
  (:require [netcdf.dods :as dods]
            [netcdf.geo-grid :as grid])
  (:use [clojure.contrib.def :only (defvar)]
        [clj-time.core :only (now interval)]
        [netcdf.dataset :only (copy-dataset find-geo-grid open-grid-dataset)]
        clj-time.format
        clojure.contrib.logging
        netcdf.dods
        netcdf.repository
        netcdf.utils
        netcdf.time))

(defmacro defvariable
  "Define and register a variable."
  [name description & attributes]
  (let [name# name description# description]
    `(defvar ~name#
       ~(assoc (apply hash-map attributes) :name (str name#) :description description#)
       ~description#)))

(defn download-variable [model variable & {:keys [reference-time root-dir]}]
  (if-let [reference-time (to-date-time (or reference-time (latest-reference-time model)))]
    (let [start-time (now)
          dataset (first (dods/find-datasets-by-url-and-reference-time (:dods model) reference-time))
          filename (variable-path model variable reference-time root-dir)]
      (info (str "           Model: " (:description model) " (" (:name model) ")"))
      (info (str "        Variable: " (:description variable) " (" (:name variable) ")"))
      (info (str "  Reference Time: " (unparse (formatters :rfc822) reference-time)))
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

(defvar gfs-variables
  [tmpsfc tcdcclm]
  "The variables of the Global Forecast System.")

(defvar wave-watch-variables
  [dirpwsfc dirswsfc htsgwsfc perpwsfc perswsfc ugrdsfc
   vgrdsfc wdirsfc windsfc wvdirsfc wvpersfc]
  "The variables of the NOAA Wave Watch III model.")
