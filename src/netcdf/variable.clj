(ns netcdf.variable
  (:use [clojure.contrib.def :only (defvar)]))

(defmacro defvariable
  "Define and register a variable."
  [name description & attributes]
  (let [name# name]
    `(def ~name# ~(assoc (apply hash-map attributes) :name (str name#) :description description))))

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
