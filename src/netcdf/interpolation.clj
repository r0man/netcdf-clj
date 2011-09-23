(ns netcdf.interpolation
  (:import incanter.Matrix
           ucar.nc2.dt.GridCoordSystem
           (javax.media.jai InterpolationBicubic InterpolationBilinear))
  (:use [incanter.core :only (matrix ncol nrow sel)]
        netcdf.location
        netcdf.coord-system))

(def ^:dynamic *interpolation* (InterpolationBilinear.))

(defmacro with-interpolation [interpolation & body]
  `(binding [*interpolation* ~interpolation]
     ~@body))

(defmulti interpolate
  (fn [^Matrix matrix ^Float x-fract ^Float y-fract]
    (keyword (str (ncol matrix) "x" (nrow matrix)))))

(defmethod interpolate :2x2 [^Matrix matrix ^Float x-fract ^Float y-fract]
  (. *interpolation* interpolate
     (sel matrix 0 0) ; the central sample
     (sel matrix 0 1) ; the sample to the right of the central sample
     (sel matrix 1 0) ; the sample below the central sample
     (sel matrix 1 1) ; the sample below and to the right of the central sample
     (float x-fract)
     (float y-fract)))

(defmethod interpolate :4x4 [^Matrix matrix ^Float x-fract ^Float y-fract]
  (. *interpolation* interpolate
     (sel matrix 0 0) ; the sample above and to the left of the central sample
     (sel matrix 0 1) ; the sample above the central sample
     (sel matrix 0 2) ; the sample above and one to the right of the central sample
     (sel matrix 0 3) ; the sample above and two to the right of the central sample
     (sel matrix 1 0) ; the sample to the left of the central sample
     (sel matrix 1 1) ; the central sample
     (sel matrix 1 2) ; the sample to the right of the central sample
     (sel matrix 1 3) ; the sample two to the right of the central sample
     (sel matrix 2 0) ; the sample below and one to the left of the central sample
     (sel matrix 2 1) ; the sample below the central sample
     (sel matrix 2 2) ; the sample below and one to the right of the central sample
     (sel matrix 2 3) ; the sample below and two to the right of the central sample
     (sel matrix 3 0) ; the sample two below and one to the left of the central sample
     (sel matrix 3 1) ; the sample two below the central sample
     (sel matrix 3 2) ; the sample two below and one to the right of the central sample
     (sel matrix 3 3) ; the sample two below and two to the right of the central sample
     (float x-fract)
     (float y-fract)))

(defn sample-location [location lat-step lon-step]
  (make-location
   (* (Math/ceil (/ (latitude location) lat-step)) lat-step)
   (* (Math/floor (/ (longitude location) lon-step)) lon-step)))

(defn sample-offsets
  "Returns the sample offsets."
  [& [width height]]
  (for [x (range 0 (or width 2))
        y (range 0 (or height width 2))]
    [x y]))

(defn sample-locations
  "Returns the sample locations for the given location"
  [^GridCoordSystem coord-system location & {:keys [width height]}]
  (if-let [start (location-on-grid coord-system location)]
    (let [lat-step (:step (latitude-axis coord-system))
          lon-step (:step (longitude-axis coord-system))
          sample-location (sample-location location lat-step lon-step)]
      (for [[x y] (sample-offsets width height)]
        (make-location
         (- (latitude sample-location) (* x lat-step))
         (+ (longitude sample-location) (* y lon-step)))))))
