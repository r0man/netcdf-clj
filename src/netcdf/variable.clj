(ns netcdf.variable)

(defmacro defvariable
  "Define and register a variable."
  [name description & attributes]
  (let [name# name]
    `(def ~name# ~(assoc (apply hash-map attributes) :name (str name#) :description description))))

(defvariable dirpwsfc
  "Primary wave direction"
  :unit "°")

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
