(ns netcdf.interpolation
  (:import javax.media.jai.InterpolationBilinear
           javax.media.jai.InterpolationBicubic)
  (:use [clojure.contrib.math :only (ceil floor)]
        [clojure.contrib.seq-utils :only (flatten)]
        incanter.core netcdf.datatype netcdf.location))

(defn central-sample-location [location lat-step lon-step]
  (make-location
   (* (ceil (/ (:latitude location) lat-step)) lat-step)
   (* (floor (/ (:longitude location) lon-step)) lon-step)))

(defn read-sample-2x2 [datatype valid-time location]
  (read-matrix datatype valid-time (central-sample-location location (:lat-step datatype) (:lon-step datatype)) :width 2 :height 2))

(defn interpolate-bilinear
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [sample (read-sample-2x2 datatype valid-time location)
          xfrac (/ (- (:longitude location) (:lon-min (meta sample))) (:lon-step (meta sample)))
          yfrac (/ (- (:lat-max (meta sample)) (:latitude location)) (:lat-step (meta sample)))
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
                  (float xfrac)
                  (float yfrac))        
        :variable (:variable datatype)))))

(defn interpolate-matrix [datatype valid-time location & options]
  (apply read-matrix datatype valid-time location (flatten (seq (assoc (apply hash-map options) :read-fn interpolate-bilinear)))))

;; (:value (read-at-location *nww3* (first (valid-times *nww3*)) (make-location 68 0)))
;; (println (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 76.5 0))))


;; (println (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 10))
;; (println (interpolate-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 10 :lat-step 0.5))

;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 68 0)))

;; (def *nww3* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))
;; (println (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 15))


;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 76.5 1)))

;; (defn matrix-interpolate-bilinear [datatype valid-time location & [width height]]
;;   (let [width (or width 10) height (or height width) ]    
;;     (matrix
;;      (for [latitude (reverse (sample-latitude (:latitude location) height (:step (lat-axis datatype))))
;;            longitude (sample-longitude (:longitude location) width (:step (lon-axis datatype)))]
;;        (:value (interpolate-bilinear datatype valid-time (make-location latitude longitude))))
;;      width)))
