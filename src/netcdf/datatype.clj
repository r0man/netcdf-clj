(ns netcdf.datatype
  (:import (ucar.nc2.dt.grid GridDataset GridAsPointDataset)
           ucar.unidata.geoloc.LatLonPointImpl
           javax.media.jai.InterpolationBilinear)
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
      (matrix
       (apply read-seq datatype valid-time options)
       (int (:size (longitude-axis datatype))))    
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

(defn grid-location
  "Returns the nearest upper left location on the grid. If the location
is exactly on the grid the same location is returned."  
  [datatype location]
  (let [lon-axis (longitude-axis datatype) lat-axis (latitude-axis datatype)]
    (make-location
     (* (int (/ (:latitude location) (:step lat-axis))) (:step lat-axis))
     (* (int (/ (:longitude location) (:step lon-axis))) (:step lon-axis)))))

(defn sample-locations [datatype valid-time location num]
  (let [location (grid-location datatype location)
        step-lat (:step (latitude-axis datatype))
        step-lon (:step (longitude-axis datatype))]
    (location-range
     (make-location (- (:latitude location) (* step-lat num)) (- (:longitude location) (* step-lon num)))
     (make-location (+ (:latitude location) (* step-lat num)) (+ (:longitude location) (* step-lon num)))
     :step-lat step-lat :step-lon step-lon)))

(defn read-sample [datatype valid-time location num]
  (map #(read-at-location datatype valid-time %) (sample-locations datatype valid-time location num)))

(defn interpolate-bilinear
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [sample (map :value (read-sample datatype valid-time location 1))]
      (. (InterpolationBilinear.)
         interpolate
         (double (nth sample 0))
         (double (nth sample 1))
         (double (nth sample 2))
         (double (nth sample 3))
         (float 0)
         (float 0))
      )))

;; (interpolate-bilinear *akw* (first (valid-times *akw*)) (make-location 77 0))

;; (def *akw* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))
;; (def *sample* (map :value (read-sample *akw* (first (valid-times *akw*)) {:latitude 0 :longitude 0} 1)))

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






