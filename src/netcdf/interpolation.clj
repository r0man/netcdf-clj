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

(defn read-sample-2x2 [datatype valid-time location]
  (let [anchor (central-sample-location location (:lat-step datatype) (:lon-step datatype))
        sample (read-matrix datatype valid-time anchor :width 2 :height 2)]
    (with-meta+ sample      
      {:x-fract (x-fract sample location)
       :y-fract (y-fract sample location)})))

(defn read-sample-4x4 [datatype valid-time location]
  (let [anchor (central-sample-location location (:lat-step datatype) (:lon-step datatype))
        anchor (make-location (+ (:latitude location) (:lat-step datatype)) (- (:longitude location) (:lon-step datatype))) 
        sample (read-matrix datatype valid-time anchor :width 4 :height 4)]
    (with-meta+ sample      
      {:x-fract (x-fract sample location)
       :y-fract (y-fract sample location)})))

;; (defn read-sample-4x4 [datatype valid-time location]
;;   (let [location (central-sample-location location (:lat-step datatype) (:lon-step datatype))]
;;     (read-matrix
;;      datatype valid-time
;;      (make-location (+ (:latitude location) (:lat-step datatype)) (- (:longitude location) (:lon-step datatype)))
;;      :width 4 :height 4)))

(defn interpolate-bilinear-2x2
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [sample (read-sample-2x2 datatype valid-time location)
          ;; xfrac (/ (- (:longitude location) (:lon-min (meta sample)))
          ;;          (- (:lon-max (meta sample)) (:lon-min (meta sample))))
          ;; yfrac (/ (- (:lat-max (meta sample)) (:latitude location))
          ;;          (- (:lat-max (meta sample)) (:lat-min (meta sample))))
          ;; xfrac (/ (- (:lon-max (meta sample)) (:longitude location))
          ;;          (- (:lon-max (meta sample)) (:lon-min (meta sample))))
          ;; yfrac (/ (- (:lat-max (meta sample)) (:latitude location))
          ;;          (- (:lat-max (meta sample)) (:lat-min (meta sample))))
          ]
      ;; (println location)
      ;; (println sample)
      ;; (println (meta sample))
      ;; (println (:lon-max (meta sample)))
      ;; (println (:lon-min (meta sample)))
      ;; (println xfrac)
      ;; (println)
      ;; (println (:lat-max (meta sample)))
      ;; (println (:lat-min (meta sample)))
      ;; (println yfrac)
      ;; (println)
      (struct-map record
        :actual-location location
        :requested-location location
        :valid-time valid-time
        :value (. (InterpolationBilinear.)
                  interpolate
                  (double (sel sample 0 0))
                  (double (sel sample 0 1))
                  (double (sel sample 1 0))
                  (double (sel sample 1 1))
                  (float (x-fract sample location))
                  (float (y-fract sample location)))        
        :variable (:variable datatype)))))

(defn interpolate-bilinear-4x4
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [sample (read-sample-4x4 datatype valid-time location)
          xfrac (/ (- (:lon-max (meta sample)) (:longitude location))
                   (- (:lon-max (meta sample)) (:lon-min (meta sample))))
          yfrac (/ (- (:lat-max (meta sample)) (:latitude location))
                   (- (:lat-max (meta sample)) (:lat-min (meta sample))))
          ]
      ;; (println location)
      ;; (println sample)
      ;; (println (meta sample))
      ;; (println (:lon-max (meta sample)))
      ;; (println (:lon-min (meta sample)))
      ;; (println xfrac)
      ;; (println)
      ;; (println (:lat-max (meta sample)))
      ;; (println (:lat-min (meta sample)))
      ;; (println yfrac)
      ;; (println)
      (struct-map record
        :actual-location location
        :requested-location location
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
                  (float xfrac)
                  (float yfrac))        
        :variable (:variable datatype)))))

(defn interpolate-matrix [datatype valid-time location & options]
  (apply read-matrix datatype valid-time location (flatten (seq (assoc (apply hash-map options) :read-fn interpolate-bilinear-4x4)))))
