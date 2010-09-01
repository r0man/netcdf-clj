(ns netcdf.coord-system
  (:import ucar.nc2.dt.GridCoordSystem))

(defn projection
  "Returns the projection of the coordinate system"
  [^GridCoordSystem coord-system] (.getProjection coord-system))

(defn x-y-index
  "Find the x and y indexes of the location."
  [^GridCoordSystem coord-system location]
  (vec (. coord-system findXYindexFromLatLon (:latitude location) (:longitude location) nil)))
