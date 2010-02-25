(ns netcdf.interpolation
  (:import javax.media.jai.InterpolationBilinear
           javax.media.jai.InterpolationBicubic)
  (:use [clojure.contrib.math :only (ceil floor)]
        netcdf.datatype netcdf.location))

;;; INTERPOLATION

(defn central-sample-location [location lat-step lon-step]
  (make-location
   (* (ceil (/ (:latitude location) lat-step)) lat-step)
   (* (floor (/ (:longitude location) lon-step)) lon-step)))

(defn sample-latitude [latitude height step]
  (let [latitude (* (int (/ latitude step)) step)]
    (range (- latitude (* (- height 2) step) step) (+ latitude step) step)))

(defn sample-longitude [longitude width step]
  (let [longitude (* (int (/ longitude step)) step)]
    (range longitude (+ longitude (* (- width 1) step) step) step)))

(defn sample-location [location & options]
  (let [options (apply hash-map options)
        lat-step (or (:lat-step options) 1)
        lon-step (or (:lon-step options) 1)
        location (central-sample-location location lat-step lon-step)]
    (for [latitude (reverse (sample-latitude (:latitude location) (:width options) lat-step))
          longitude (sample-longitude (:longitude location) (:height options) lon-step)]
      (make-location latitude longitude))))

(defn location->sample-2x2 [location & options]
  (let [options (apply hash-map options)]
    (zipmap
     [:s00 :s01 :s10 :s11]
     (sample-location location :lat-step (:lat-step options) :lon-step (:lon-step options) :width 2 :height 2))))

(defn location->sample-4x4 [location & options]
  (let [options (apply hash-map options)]
    (zipmap
     [:s__ :s_0 :s_1 :s_2 :s0_ :s00 :s01 :s02 :s1_ :s10 :s11 :s12 :s2_ :s20 :s21 :s22]
     (sample-location location :lat-step (:lat-step options) :lon-step (:lon-step options) :width 4 :height 4))))

(defn read-sample-2x2 [datatype valid-time location]
  (let [locations (location->sample-2x2 location :lat-step (:lat-step (lat-axis datatype)) :lon-step (:step (lon-axis datatype)))]
    (with-meta (zipmap (keys locations) (map #(read-at-location datatype valid-time %) (vals locations)))
      {:lat-min (:latitude (:s11 locations))
       :lat-max (:latitude (:s01 locations))
       :lon-min (:longitude (:s00 locations))
       :lon-max (:longitude (:s01 locations))
       })
    ))

;; (sample-location (make-location 0 0) :lat-step 1 :lon-step 1 :width 2 :height 2)
;; (sample-location (make-location 76.5 1) 1 1.25 2 2)
;; (sample-location (make-location 0 0) 1 1 4 4)

;;  1,-1   1,0   1,1
;;  0,-1   0,0   0,1
;; -1,-1  -1,0  -1,1

;; :s__ :s_0 :s_1 :_s2
;; :s0_ :s00 :s01 :s02
;; :s1_ :s10 :s11 :s12
;; :s2_ :s20 :s21 :s22

(defn read-sample [datatype valid-time location]
  (let [locations (sample-location location (:step (lat-axis datatype)) (:step (lon-axis datatype)) 4 4)]
    (with-meta (map #(read-at-location datatype valid-time %) locations)
      ;; TODO: REALLY?
      {:lat-min (:latitude (first locations))
       :lat-max (:latitude (last locations))
       :lon-min (:longitude (first locations))
       :lon-max (:longitude (last locations))})))

;; (read-sample *akw* (first (valid-times *akw*)) {:latitude 0 :longitude 0})

(defn interpolate-bilinear
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [sample (read-sample-2x2 datatype valid-time location)
          xfrac (/ (- (:latitude location) (:lat-min (meta sample))) 1)
          yfrac (/ (- (:longitude location) (:lon-min (meta sample))) 1.25)
          ;; xfrac (- (:lat-max (meta sample)) (:latitude location))
          ;; yfrac (- (:lon-max (meta sample) (:longitude location)))
          ]
      ;; (println (:lat-min (meta sample)))
      ;; (println (:lat-max (meta sample)))
      ;; (println xfrac)
      ;; (println)
      ;; (println (:lon-min (meta sample)))
      ;; (println (:lon-max (meta sample)))
      ;; (println yfrac)
      ;; (println)
      (struct-map record
        :actual-location location
        :requested-location location
        :valid-time valid-time
        :value (. (InterpolationBilinear.)
                  interpolate
                  (double (:value (:s00 sample)))
                  (double (:value (:s01 sample)))
                  (double (:value (:s10 sample)))
                  (double (:value (:s11 sample)))
                  ;; (double (nth values 4))
                  ;; (double (nth values 5))
                  ;; (double (nth values 6))
                  ;; (double (nth values 7))
                  ;; (double (nth values 8))
                  ;; (double (nth values 9))
                  ;; (double (nth values 10))
                  ;; (double (nth values 11))
                  ;; (double (nth values 12))
                  ;; (double (nth values 13))
                  ;; (double (nth values 14))
                  ;; (double (nth values 15))
                  (float xfrac)
                  (float yfrac))        
        :variable (:variable datatype)))))

(def *nww3* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))
;; (println (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 15))

;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 75 1.25)))
;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 76.5 1)))

;; (defn matrix-interpolate-bilinear [datatype valid-time location & [width height]]
;;   (let [width (or width 10) height (or height width) ]    
;;     (matrix
;;      (for [latitude (reverse (sample-latitude (:latitude location) height (:step (lat-axis datatype))))
;;            longitude (sample-longitude (:longitude location) width (:step (lon-axis datatype)))]
;;        (:value (interpolate-bilinear datatype valid-time (make-location latitude longitude))))
;;      width)))
