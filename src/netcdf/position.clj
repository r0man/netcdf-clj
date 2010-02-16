(ns netcdf.position)

(defstruct position :x :y)

(defn make-position [x y]
  (struct position x y))

(defn position? [position]
  (and (:x position) (:y position) position))
