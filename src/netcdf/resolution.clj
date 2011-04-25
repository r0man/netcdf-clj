(ns netcdf.resolution)

(defrecord Resolution [width height])

(defn make-resolution
  "Make new a resolution map."
  [width height] (Resolution. width height))

(defn sort-resolutions
  "Returns a sorted sequence of the resolutions."
  [resolutions] (sort-by #(* (:width %) (:height %)) resolutions))

(defn max-resolution
  "Returns the max/highest resolution of the given resolutions."
  [resolutions] (first (sort-resolutions resolutions)))

(defn min-resolution
  "Returns the min/lowest resolution of the given resolutions."
  [resolutions] (last (sort-resolutions resolutions)))

(defn merge-resolutions
  "Merge the resolutions."
  [resolutions]
  (make-resolution
   (apply min (map :width resolutions))
   (apply min (map :height resolutions))))