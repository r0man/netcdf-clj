(ns netcdf.interpolation
  (:import (javax.media.jai InterpolationBicubic InterpolationBilinear))
  (:use [incanter.core :only (matrix ncol nrow sel)]))

(def *interpolation* (InterpolationBilinear.))

(defmacro with-interpolation [interpolation & body]
  `(binding [*interpolation* ~interpolation]
     ~@body))

(defmulti interpolate
  (fn [#^Matrix matrix #^Float x-fract #^Float y-fract]
    (keyword (str (ncol matrix) "x" (nrow matrix)))))

(defmethod interpolate :2x2 [#^Matrix matrix #^Float x-fract #^Float y-fract]
  (. *interpolation* interpolate
     (sel matrix 0 0) ; the central sample
     (sel matrix 0 1) ; the sample to the right of the central sample
     (sel matrix 1 0) ; the sample below the central sample
     (sel matrix 1 1) ; the sample below and to the right of the central sample
     (float x-fract)
     (float y-fract)))

(defmethod interpolate :4x4 [#^Matrix matrix #^Float x-fract #^Float y-fract]
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

;; (defn central-sample-location [location lat-step lon-step]
;;   (make-location
;;    (* (ceil (/ (:latitude location) lat-step)) lat-step)
;;    (* (floor (/ (:longitude location) lon-step)) lon-step)))

;; (defn x-fract [sample location]
;;   (/ (- (:longitude location) (:lon-min (meta sample)))
;;      (- (:lon-max (meta sample)) (:lon-min (meta sample)))))

;; (defn y-fract [sample location]
;;   (/ (- (:lat-max (meta sample)) (:latitude location))
;;      (- (:lat-max (meta sample)) (:lat-min (meta sample)))))

;; (defn read-sample-2x2 [datatype valid-time location & options]
;;   (let [anchor (central-sample-location location (:lat-step datatype) (:lon-step datatype))
;;         sample (apply read-matrix datatype valid-time anchor (flatten (seq (merge (apply hash-map options) {:width 2 :height 2}))))]
;;     (with-meta+ sample      
;;       {:x-fract (x-fract sample location)
;;        :y-fract (y-fract sample location)})))

;; (defn read-sample-4x4 [datatype valid-time location & options]
;;   (let [anchor (central-sample-location location (:lat-step datatype) (:lon-step datatype))
;;         anchor (make-location (+ (:latitude location) (:lat-step datatype)) (- (:longitude location) (:lon-step datatype)))
;;         sample (apply read-matrix datatype valid-time anchor (flatten (seq (merge (apply hash-map options) {:width 4 :height 4}))))]
;;     (with-meta+ sample      
;;       {:x-fract (x-fract sample location)
;;        :y-fract (y-fract sample location)})))

;; (defn interpolate-bicubic-2x2
;;   "Read the NetCDF datatype for the given time and location."
;;   [datatype valid-time location & options]
;;   (if location
;;     (let [sample (apply read-sample-2x2 datatype valid-time location options)]
;;       (struct-map record
;;         :variable (:variable datatype)
;;         :location location
;;         :valid-time valid-time
;;         :value (. (InterpolationBicubic. 4)
;;                   interpolate
;;                   (double (sel sample 0 0))
;;                   (double (sel sample 0 1))
;;                   (double (sel sample 1 0))
;;                   (double (sel sample 1 1))
;;                   (float (x-fract sample location))
;;                   (float (y-fract sample location)))))))

;; (defn interpolate-bilinear-2x2
;;   "Read the NetCDF datatype for the given time and location."
;;   [datatype valid-time location & options]
;;   (if location
;;     (let [sample (apply read-sample-2x2 datatype valid-time location options)]
;;       (struct-map record
;;         :variable (:variable datatype)
;;         :location location
;;         :valid-time valid-time
;;         :value (. (InterpolationBilinear.)
;;                   interpolate
;;                   (double (sel sample 0 0))
;;                   (double (sel sample 0 1))
;;                   (double (sel sample 1 0))
;;                   (double (sel sample 1 1))
;;                   (float (x-fract sample location))
;;                   (float (y-fract sample location)))))))

;; (defn interpolate-bilinear-4x4
;;   "Read the NetCDF datatype for the given time and location."
;;   [datatype valid-time location & options]
;;   (if location
;;     (let [sample (apply read-sample-4x4 datatype valid-time location options)]
;;       (struct-map record
;;         :variable (:variable datatype)
;;         :location location
;;         :valid-time valid-time
;;         :value (. (InterpolationBilinear.)
;;                   interpolate
;;                   (double (sel sample 0 0))
;;                   (double (sel sample 0 1))
;;                   (double (sel sample 0 2))
;;                   (double (sel sample 0 3))
;;                   (double (sel sample 1 0))
;;                   (double (sel sample 1 1))
;;                   (double (sel sample 1 2))
;;                   (double (sel sample 1 3))
;;                   (double (sel sample 2 0))
;;                   (double (sel sample 2 1))
;;                   (double (sel sample 2 2))
;;                   (double (sel sample 2 3))
;;                   (double (sel sample 3 0))
;;                   (double (sel sample 3 1))
;;                   (double (sel sample 3 2))
;;                   (double (sel sample 3 3))
;;                   (float (x-fract sample location))
;;                   (float (y-fract sample location)))))))

;; (defn interpolate-matrix [datatype valid-time location & options]
  ;; (apply read-matrix datatype valid-time location (flatten (seq (assoc (apply hash-map options) :read-fn interpolate-bilinear-4x4)))))

