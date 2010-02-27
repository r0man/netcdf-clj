(ns netcdf.interpolation
  (:import javax.media.jai.InterpolationBilinear
           javax.media.jai.InterpolationBicubic)
  (:use [clojure.contrib.math :only (ceil floor)]
        [clojure.contrib.seq-utils :only (flatten)]
        incanter.core netcdf.datatype netcdf.location))

(defn with-meta+
  "Returns an object of the same type and value as obj, with map m
  merged into the object's existing metadata."
  [obj m] (with-meta obj (merge (meta obj) m)))

(defn central-sample-location [location lat-step lon-step]
  (make-location
   (* (ceil (/ (:latitude location) lat-step)) lat-step)
   (* (floor (/ (:longitude location) lon-step)) lon-step)))

(defn x-fract [sample location]
  (/ (- (:longitude location) (:lon-min (meta sample)))
     (- (:lon-max (meta sample)) (:lon-min (meta sample)))))

(defn y-fract [sample location]
  (/ (- (:lat-max (meta sample)) (:latitude location))
     (- (:lat-max (meta sample)) (:lat-min (meta sample)))))

(defn read-sample-2x2 [datatype valid-time location & options]
  (let [anchor (central-sample-location location (:lat-step datatype) (:lon-step datatype))
        sample (apply read-matrix datatype valid-time anchor (flatten (seq (merge (apply hash-map options) {:width 2 :height 2}))))]
    (with-meta+ sample      
      {:x-fract (x-fract sample location)
       :y-fract (y-fract sample location)})))

(defn read-sample-4x4 [datatype valid-time location & options]
  (let [anchor (central-sample-location location (:lat-step datatype) (:lon-step datatype))
        anchor (make-location (+ (:latitude location) (:lat-step datatype)) (- (:longitude location) (:lon-step datatype)))
        sample (apply read-matrix datatype valid-time anchor (flatten (seq (merge (apply hash-map options) {:width 4 :height 4}))))]
    (with-meta+ sample      
      {:x-fract (x-fract sample location)
       :y-fract (y-fract sample location)})))

(defn interpolate-bicubic-2x2
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location & options]
  (if location
    (let [sample (apply read-sample-2x2 datatype valid-time location options)]
      (struct-map record
        :variable (:variable datatype)
        :location location
        :valid-time valid-time
        :value (. (InterpolationBicubic. 4)
                  interpolate
                  (double (sel sample 0 0))
                  (double (sel sample 0 1))
                  (double (sel sample 1 0))
                  (double (sel sample 1 1))
                  (float (x-fract sample location))
                  (float (y-fract sample location)))))))

(defn interpolate-bilinear-2x2
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location & options]
  (if location
    (let [sample (apply read-sample-2x2 datatype valid-time location options)]
      (struct-map record
        :variable (:variable datatype)
        :location location
        :valid-time valid-time
        :value (. (InterpolationBilinear.)
                  interpolate
                  (double (sel sample 0 0))
                  (double (sel sample 0 1))
                  (double (sel sample 1 0))
                  (double (sel sample 1 1))
                  (float (x-fract sample location))
                  (float (y-fract sample location)))))))

(defn interpolate-bilinear-4x4
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location & options]
  (if location
    (let [sample (apply read-sample-4x4 datatype valid-time location options)]
      (struct-map record
        :variable (:variable datatype)
        :location location
        :valid-time valid-time
        :value (. (InterpolationBilinear.)
                  interpolate
                  (double (sel sample 0 0))
                  (double (sel sample 0 1))
                  (double (sel sample 0 2))
                  (double (sel sample 0 3))
                  (double (sel sample 1 0))
                  (double (sel sample 1 1))
                  (double (sel sample 1 2))
                  (double (sel sample 1 3))
                  (double (sel sample 2 0))
                  (double (sel sample 2 1))
                  (double (sel sample 2 2))
                  (double (sel sample 2 3))
                  (double (sel sample 3 0))
                  (double (sel sample 3 1))
                  (double (sel sample 3 2))
                  (double (sel sample 3 3))
                  (float (x-fract sample location))
                  (float (y-fract sample location)))))))

(defn interpolate-matrix [datatype valid-time location & options]
  (apply read-matrix datatype valid-time location (flatten (seq (assoc (apply hash-map options) :read-fn interpolate-bilinear-4x4)))))
