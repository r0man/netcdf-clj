(ns netcdf.variable)

(defmacro defvariable
  "Define and register a variable."
  [name & [description unit]]
  (let [name# name]
    `(def ~name# ~{:name (str name#) :description description :unit unit})))

(defvariable dirpwsfc "Primary wave direction" "deg")
(defvariable dirpwsfc "Primary wave direction" "deg")
(defvariable dirswsfc "Secondary wave direction" "deg")
(defvariable htsgwsfc "Significant height of combined wind waves and swell" "m")
(defvariable perpwsfc "Primary wave mean period" "s")
(defvariable perswsfc "Secondary wave mean period" "s")
(defvariable ugrdsfc  "U-component of wind" "m/s")
(defvariable vgrdsfc  "V-component of wind" "m/s")
(defvariable wdirsfc  "Wind direction" "deg")
(defvariable windsfc  "Wind speed" "m/s")
(defvariable wvdirsfc "Direction of wind waves" "deg")
(defvariable wvpersfc "Mean period of wind waves" "s")
