(ns netcdf.datatype
  (:import (ucar.nc2.dt.grid GridDataset GridAsPointDataset)
           ucar.unidata.geoloc.LatLonPointImpl)
  (:use [clojure.contrib.math :only (ceil floor)] 
        incanter.core netcdf.location))

(defstruct datatype :dataset-uri :variable :service)
(defstruct record :actual-location :distance :unit :valid-time :value :variable)

(defn bounding-box
  "Returns the bounding box of the datatype."
  [datatype]
  (.. (:service datatype) getCoordinateSystem getLatLonBoundingBox))

(defn description [datatype]
  (.. (:service datatype) getVariable getDescription))

(defn lat-axis [datatype]
  (let [axis (.. (:service datatype) getCoordinateSystem getYHorizAxis) bounds (bounding-box datatype)]
    {:lat-min (.getLatMin bounds)
     :lat-max (.getLatMax bounds)
     :lat-size (.getSize axis)
     :lat-step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn lon-axis [datatype]
  (let [axis (.. (:service datatype) getCoordinateSystem getXHorizAxis) bounds (bounding-box datatype)]
    {:lon-min (.getLonMin bounds)
     :lon-max (.getLonMax bounds)
     :lon-size (.getSize axis)
     :lon-step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn axis [datatype]
  (merge (lat-axis datatype) (lon-axis datatype)))

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
    (let [datatype (assoc datatype :service (. (. GridDataset open (:dataset-uri datatype)) findGridDatatype (:variable datatype)))]
      (merge datatype (axis datatype)))))

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
        :actual-location (make-location (.lat data) (.lon data))
        :requested-location location
        :unit (.getUnitsString (:service datatype))
        :valid-time valid-time
        :value (.dataValue data)
        :variable (:variable datatype)))))

(defn read-seq [datatype valid-time location & options]
  (let [options (apply hash-map options)
        width (or (:width options) 2)
        height (or (:height options) width)
        locations (location-rect location :width width :height height :lat-step (:lat-step datatype) :lon-step (:lon-step datatype))]
    (with-meta (map #(read-at-location datatype valid-time %) locations)
      (merge {:description (description datatype)
              :valid-time valid-time
              :variable (:variable datatype)
              :lat-min (:latitude (last locations))
              :lat-max (:latitude (first locations))
              :lat-size width
              :lat-step (:lat-step (lat-axis datatype))
              :lon-min (:longitude (first locations))
              :lon-max (:longitude (last locations))
              :lon-size height
              :lon-step (:lon-step (lon-axis datatype))}))))

;; (defn read-matrix
;;   "Read the whole datatype at valid-time as matrix."
;;   [datatype valid-time & options]
;;   (let [options (apply hash-map options)]
;;     (with-meta
;;       (trans (matrix
;;         (apply read-seq datatype valid-time options)
;;         (int (:size (lon-axis datatype)))))    
;;       (merge {:description (description datatype)
;;               :valid-time valid-time
;;               :variable (:variable datatype)} (axis datatype)))))

(defn valid-times
  "Returns the valid times in the NetCDF datatype."
  [datatype]
  (if (datatype-open? datatype)
    (.. (.getCoordinateSystem (:service datatype)) getTimeAxis1D getTimeDates)
    (valid-times (open-datatype datatype))))


;; (def *nww3* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))

;; (defn read-matrix [datatype valid-time location & options]
;;   (let [sequence (read-seq datatype valid-time location width height)]
;;     (with-meta (matrix sequence (or width 2)) {})))

;; (println (read-seq *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 1))
;; (println (meta (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0))))

;; (defn matrix-read-at-location [datatype valid-time location & [width height]]
;;   (let [width (or width 10) height (or height width)]    
;;     (matrix
;;      (for [latitude (reverse (range (:latitude location) (+ (:latitude location) height) (:step (lat-axis datatype))))
;;            longitude (range (:longitude location) (+ (:longitude location) width) (:step (lon-axis datatype)))]
;;        (:value (read-at-location datatype valid-time (make-location latitude longitude))))
;;      width)))

;; (defn matrix-read-at-location [datatype valid-time location & [width height]]
;;   (let [width (or width 10) height (or height width)]    
;;     (matrix
;;      (for [latitude (reverse (sample-latitude (:latitude location) height (:step (lat-axis datatype))))
;;            longitude (sample-longitude (:longitude location) width (:step (lon-axis datatype)))]
;;        (:value (read-at-location datatype valid-time (make-location latitude longitude))))
;;      width)))


;; (println (matrix-read-at-location *nww3* (first (valid-times *nww3*)) (make-location 78 0) 7))
;; (println (matrix-interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 78 0) 7))
;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 77 0)))

;; (defn matrix-read [datatype valid-time & options]
;;   (let [options (apply hash-map options)
;;         read-fn (or (:read-fn options read-at-location))]))

;; (def *formatter* (java.text.DecimalFormat. "#0.00"))



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
;;   (let [step-lat (:step (lat-axis datatype)) step-lon (:step (lon-axis datatype))]
;;     (for [latitude (reverse (range (:latitude location) (+ (:latitude location) (* num step-lat)) step-lat))
;;           longitude (range (:longitude location) (+ (:longitude location) (* num step-lon)) step-lon)]
;;       (make-location latitude longitude))))

;; ;; (defn location-range [lat1 lon1 lat2 lon2]
;;   (let [step-lat (:step (lat-axis datatype)) step-lon (:step (lon-axis datatype))]
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
;;         step-lat (:step (lat-axis datatype))
;;         step-lon (:step (lon-axis datatype))]
;;     (location-range
;;      (make-location (- (:latitude location) (* step-lat num)) (- (:longitude location) (* step-lon num)))
;;      (make-location (+ (:latitude location) (* step-lat num)) (+ (:longitude location) (* step-lon num)))
;;      :step-lat step-lat :step-lon step-lon)))

;; (defn sample-locations [datatype location width height]
;;   (let [location (central-sample-location datatype location)
;;         step-lat (:step (lat-axis datatype))
;;         step-lon (:step (lon-axis datatype))]
;;     (for [
;;           latitude (range (- (:latitude location) (* (/ width 2) step-lat)) (+ (:latitude location) (* (/ width 2) step-lat)) step-lat)
;;           longitude (range (- (:longitude location) (* (/ height 2) step-lon)) (+ (:longitude location) (* (/ height 2) step-lon)) step-lon)
;; ]
;;       [latitude longitude]
;;       )))
