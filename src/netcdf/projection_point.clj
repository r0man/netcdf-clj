(ns netcdf.projection-point
  (:import (ucar.unidata.geoloc ProjectionPoint ProjectionPointImpl)))

(defn projection-point?
  "Returns true if arg is a projection point, otherwise false."
  [arg] (isa? (class arg) ProjectionPoint))

(defn make-projection-point [x y]
  (ProjectionPointImpl. x y))
