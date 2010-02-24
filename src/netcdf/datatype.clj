(ns netcdf.datatype
  (:import (ucar.nc2.dt.grid GridDataset GridAsPointDataset)
           ucar.unidata.geoloc.LatLonPointImpl
           javax.media.jai.InterpolationBilinear
           javax.media.jai.InterpolationBicubic)
  (:use incanter.core netcdf.location))

(defstruct datatype :dataset-uri :variable :service)
(defstruct record :actual-location :distance :unit :valid-time :value :variable)

(defn bounding-box
  "Returns the bounding box of the datatype."
  [datatype]
  (.. (:service datatype) getCoordinateSystem getLatLonBoundingBox))

(defn description [datatype]
  (.. (:service datatype) getVariable getDescription))

(defn latitude-axis [datatype]
  (let [axis (.. (:service datatype) getCoordinateSystem getYHorizAxis)
        bounds (bounding-box datatype)]
    {:min (.getLatMin bounds)
     :max (.getLatMax bounds)
     :size (.getSize axis)
     :step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn longitude-axis [datatype]
  (let [axis (.. (:service datatype) getCoordinateSystem getXHorizAxis)
        bounds (bounding-box datatype)]
    {
     :min (.getLonMin bounds)
     :max (.getLonMax bounds)
     :size (.getSize axis)
     :step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn make-datatype
  "Make a NetCDF datatype."
  [dataset-uri variable]
  (struct datatype dataset-uri variable))

(defn datatype-open?
  "Returns true if the datatype is open, else false."
  [datatype]
  (not (nil? (:service datatype))))

(defn open-datatype
  "Open the NetCDF datatype."
  [datatype]
  (if-not (datatype-open? datatype)
    (let [grid-dataset (. GridDataset open (:dataset-uri datatype))]
      (assoc datatype :service (. grid-dataset findGridDatatype (:variable datatype))))))

(defn time-index
  "Returns the time index for valid-time."
  [datatype valid-time]
  (. (. (.getCoordinateSystem (:service datatype)) getTimeAxis1D) findTimeIndexFromDate valid-time))>

(defn- read-data [datatype valid-time location]
  (let [datatype (:service datatype) dataset (GridAsPointDataset. [datatype])]
    (if (and (:altitude location) (. dataset hasVert datatype (:altitude location)))
      (. dataset readData datatype valid-time (:altitude location) (:latitude location) (:longitude location))
      (. dataset readData datatype valid-time (:latitude location) (:longitude location)))))

(defn read-at-location
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [data (read-data datatype valid-time location)]
      (struct-map record
        :actual-location (make-location (.lat data) (.lon data) (.z data))
        :requested-location location
        :unit (.getUnitsString (:service datatype))
        :valid-time valid-time
        :value (.dataValue data)
        :variable (:variable datatype)))))

(defn read-seq
  "Read the whole datatype at valid-time as sequence."
  [datatype valid-time & options]
  (let [options (apply hash-map options)]
    (with-meta
      (seq (.copyTo1DJavaArray (.readYXData (:service datatype) (time-index datatype valid-time) (or (:z options) 0))))
      {:description (description datatype)
       :latitude-axis (latitude-axis datatype)
       :longitude-axis (longitude-axis datatype)
       :valid-time valid-time
       :variable (:variable datatype)})))

(defn read-matrix
  "Read the whole datatype at valid-time as matrix."
  [datatype valid-time & options]
  (let [options (apply hash-map options)]
    (with-meta
      (trans (matrix
        (apply read-seq datatype valid-time options)
        (int (:size (longitude-axis datatype)))))    
      {:description (description datatype)
       :latitude-axis (latitude-axis datatype)
       :longitude-axis (longitude-axis datatype)
       :valid-time valid-time
       :variable (:variable datatype)})))

(defn valid-times
  "Returns the valid times in the NetCDF datatype."
  [datatype]
  (if (datatype-open? datatype)
    (.. (.getCoordinateSystem (:service datatype)) getTimeAxis1D getTimeDates)
    (valid-times (open-datatype datatype))))

(defn central-sample-location [location step-lat step-lon]
  (make-location
   (* (int (/ (:latitude location) step-lat)) step-lat)
   (* (int (/ (:longitude location) step-lon)) step-lon)))

;; (defn sample-latitude [latitude height step]
;;   (let [latitude (* (int (/ latitude step)) step)]
;;     (range (- latitude (* (- height 2) step) step) (+ latitude step) step)))

(defn sample-latitude [latitude height step]
  (let [latitude (* (int (/ latitude step)) step)]
    (range (- latitude (* (- height 2) step) step) (+ latitude step) step)))

(defn sample-longitude [longitude width step]
  (let [longitude (* (int (/ longitude step)) step)]
    (range longitude (+ longitude (* (- width 1) step) step) step)))

(defn sample-location [location & options]
  (let [options (apply hash-map options)
        step-lat (or (:step-lat options) 1)
        step-lon (or (:step-lon options) 1)
        location (central-sample-location location step-lat step-lon)]
    (for [latitude (reverse (sample-latitude (:latitude location) (:width options) step-lat))
          longitude (sample-longitude (:longitude location) (:height options) step-lon)]
      (make-location latitude longitude))))

(defn location->sample-2x2 [location & options]
  (let [options (apply hash-map options)]
    (zipmap
     [:s00 :s01 :s10 :s11]
     (sample-location location :step-lat (:step-lat options) :step-lon (:step-lon options) :width 2 :height 2))))

(defn location->sample-4x4 [location & options]
  (let [options (apply hash-map options)]
    (zipmap
     [:s__ :s_0 :s_1 :s_2 :s0_ :s00 :s01 :s02 :s1_ :s10 :s11 :s12 :s2_ :s20 :s21 :s22]
     (sample-location location :step-lat (:step-lat options) :step-lon (:step-lon options) :width 4 :height 4))))

(defn read-sample-2x2 [datatype valid-time location]
  (let [locations (location->sample-2x2 location :step-lat (:step-lat (latitude-axis datatype)) :step-lon (:step (longitude-axis datatype)))]
    (with-meta (zipmap (keys locations) (map #(read-at-location datatype valid-time %) (vals locations)))
      {:lat-min (:latitude (:s11 locations))
       :lat-max (:latitude (:s01 locations))
       :lon-min (:longitude (:s00 locations))
       :lon-max (:longitude (:s01 locations))
       })
    ))

;; (sample-location (make-location 0 0) :step-lat 1 :step-lon 1 :width 2 :height 2)
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
  (let [locations (sample-location location (:step (latitude-axis datatype)) (:step (longitude-axis datatype)) 4 4)]
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
          xfrac (- (:latitude location) (:lat-min (meta sample)) 1)
          yfrac (- (:longitude location) (:lon-min (meta sample)) 1.25)
          ;; xfrac (- (:latitude location) (:lat-min (meta sample)) 1)
          ;; yfrac (- (:longitude location) (:lon-min (meta sample)) 1.25)
          ]
      ;; (println (count values))
      (println (:lat-min (meta sample)))
      (println (:lat-max (meta sample)))
      (println (:lon-min (meta sample)))
      (println (:lon-max (meta sample)))
      (println xfrac)
      (println yfrac)
      ;; (println (count sample))
      (println)
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

;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 76.2 0)))


;; (def *nww3* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))

;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 74 1.25)))
;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 76 0)))
;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 75 0)))
;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 75 2)))

;; (sample-location (make-location 0 0) 1 1 0 0)


;; (sample-latitude-range 2.6 1.25 3)
;; (sample-longitude-range 71.5 1 3)

;; (sample-locations *akw* (make-location 76 1.25) 1 1)
;; (sample-locations *akw* (make-location 0 0) 2 2)
;; (println (sample-locations *akw* (make-location 76 1.25) 2 2))

;; (map :requested-location (read-sample *akw* (first (valid-times *akw*)) {:latitude 0 :longitude 0} 1))
;; (meta (read-sample *akw* (first (valid-times *akw*)) {:latitude 0 :longitude 0}))



;; (:value (interpolate-bilinear *akw* (first (valid-times *akw*)) (make-location 78 0)))
;; (:value (interpolate-bilinear *akw* (first (valid-times *akw*)) (make-location 76 2)))
;; (:value (interpolate-bilinear *akw* (first (valid-times *akw*)) (make-location 70 0)))

;; (:value (interpolate-bilinear *akw* (first (valid-times *akw*)) (make-location 76.9 0)))

;; (:value (read-at-location *akw* (first (valid-times *akw*)) (make-location 78 0)))
;; (:value (read-at-location *akw* (first (valid-times *akw*)) (make-location 77 0)))

;; (. (InterpolationBilinear.)
;;                   interpolate
;;                   (double 1.8)
;;                   (double 1.6)
;;                   (double 2.3)
;;                   (double 2.3)
;;                   (float 0.8)
;;                   (float 0))

;; (def *akw* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))
;; (def *sample* (map :value (read-sample *akw* (first (valid-times *akw*)) {:latitude 0 :longitude 0})))

;; *sample*
;; (partition 2 *sample*)
;; (into-array *sample*)
;; (into-array (partition 2 *sample*))

;; (to-array-2d (partition 2 (double-array *sample*)))

;; (. (InterpolationBilinear.) interpolate 0 0 0 0 0 0)
;; (. (InterpolationBilinear.) interpolateH (double-array *sample*) (float 0))

;; (. (InterpolationBilinear.) interpolate (doubles (double-array *sample*)) (float 0) (float 0))

;; (double-array *sample*)
;; (doubles (double-array *sample*))
;; (doubles (doubles (double-array *sample*)))
;; (doubles *sample*)

;; (to-array-2d [[1 2] [1 2]])
;; (double-array )
;; (to-array-2d (double-array *sample*))

;; (doubles (into-array [(double 0.0)]))

;; (sample-locations *akw* (first (valid-times *akw*)) {:latitude 0 :longitude 0})
;; (sample-locations *akw* (first (valid-times *akw*)) {:latitude 78 :longitude 0})

;; (defn location-rect [datatype location num]
;;   (let [step-lat (:step (latitude-axis datatype)) step-lon (:step (longitude-axis datatype))]
;;     (for [latitude (reverse (range (:latitude location) (+ (:latitude location) (* num step-lat)) step-lat))
;;           longitude (range (:longitude location) (+ (:longitude location) (* num step-lon)) step-lon)]
;;       (make-location latitude longitude))))

;; ;; (defn location-range [lat1 lon1 lat2 lon2]
;;   (let [step-lat (:step (latitude-axis datatype)) step-lon (:step (longitude-axis datatype))]
;;     (for [latitude (reverse (range (:latitude location) (+ (:latitude location) (* num step-lat)) step-lat))
;;           longitude (range (:longitude location) (+ (:longitude location) (* num step-lon)) step-lon)]
;;       (make-location latitude longitude))))


  
;;   (map #(read-at-location datatype valid-time %) (location-rect datatype location 2))
;; (location-rect *akw* {:latitude 0 :longitude 0} 3)


 ;; (map :value (read-samples *akw* (first (valid-times *akw*)) {:latitude 76 :longitude 0}))



;; (defn read-range [datatype valid-time lat-range lon-range]
;;   (for [ latitude (reverse lat-range) longitude lon-range]
;;     (read-at-location datatype valid-time {:latitude latitude :longitude longitude})))

;; (into-array (map :value (read-range *akw* (first (valid-times *akw*)) (range 75 79 1) (range 0 5.0 1.25))))

;; (reverse (map :value (read-range *akw* (first (valid-times *akw*)) (range 75 79 1) (range 0 5.0 1.25))))

;; (println
;;  (matrix
;;   (map :value (read-range *akw* (first (valid-times *akw*)) (range 75 79 1) (range 0 5.0 1.25)))
;;   4))






;; (defn sample-locations [datatype location num]
;;   (let [location (central-sample-location datatype location)
;;         step-lat (:step (latitude-axis datatype))
;;         step-lon (:step (longitude-axis datatype))]
;;     (location-range
;;      (make-location (- (:latitude location) (* step-lat num)) (- (:longitude location) (* step-lon num)))
;;      (make-location (+ (:latitude location) (* step-lat num)) (+ (:longitude location) (* step-lon num)))
;;      :step-lat step-lat :step-lon step-lon)))

;; (defn sample-locations [datatype location width height]
;;   (let [location (central-sample-location datatype location)
;;         step-lat (:step (latitude-axis datatype))
;;         step-lon (:step (longitude-axis datatype))]
;;     (for [
;;           latitude (range (- (:latitude location) (* (/ width 2) step-lat)) (+ (:latitude location) (* (/ width 2) step-lat)) step-lat)
;;           longitude (range (- (:longitude location) (* (/ height 2) step-lon)) (+ (:longitude location) (* (/ height 2) step-lon)) step-lon)
;; ]
;;       [latitude longitude]
;;       )))
