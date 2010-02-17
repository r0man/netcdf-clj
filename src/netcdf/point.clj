(ns netcdf.point)

(defstruct point :x :y)

(defn make-point [x y]
  (struct point x y))

(defn point? [point]
  (and (:x point) (:y point) point))
