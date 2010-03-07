(ns netcdf.datatype
  (:import (ucar.nc2.dt.grid GeoGrid GridDataset GridAsPointDataset)
           (ucar.unidata.geoloc LatLonPointImpl LatLonRect ProjectionPointImpl)
           clojure.lang.PersistentStructMap
           incanter.Matrix
           ucar.ma2.Range)
  (:use [clojure.contrib.math :only (ceil floor)]
        [clojure.contrib.repl-utils :only (show)]
        clojure.contrib.profile
        incanter.core netcdf.location netcdf.utils))

(defstruct datatype :dataset-uri :variable :service)
(defstruct record :actual-location :distance :unit :valid-time :value :variable)

(defn coord-system
  "Returns the coordinate system of the datatype."
  [datatype] (.. (or (:service datatype) datatype) getCoordinateSystem))

(defn bounding-box
  "Returns the bounding box of the datatype."
  [datatype] (. (coord-system datatype) getLatLonBoundingBox))

(defn description
  "Returns the description of the datatype."
  [datatype] (.. (or (:service datatype) datatype) getVariable getDescription))

(defn latitude-axis
  "Returns the latitude axis of the datatype."
  [datatype]
  (let [axis (. (coord-system datatype) getYHorizAxis) bounds (bounding-box datatype)]
    {:lat-min (.getLatMin bounds)
     :lat-max (.getLatMax bounds)
     :lat-size (int (.getSize axis))
     :lat-step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn longitude-axis
  "Returns the longitude axis of the datatype."
  [datatype]
  (let [axis (. (coord-system datatype) getXHorizAxis) bounds (bounding-box datatype)]
    {:lon-min (.getLonMin bounds)
     :lon-max (.getLonMax bounds)
     :lon-size (int (.getSize axis))
     :lon-step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn axis
  "Returns the latitude and longitude axis of the datatype."
  [datatype] (merge (latitude-axis datatype) (longitude-axis datatype)))

(defn time-index
  "Returns the time index for valid-time."
  [datatype valid-time]
  (. (. (coord-system datatype) getTimeAxis1D) findTimeIndexFromDate valid-time))

(defn make-datatype
  "Make a NetCDF datatype."
  [dataset-uri variable]
  (struct datatype dataset-uri variable))

(defn projection
  "Returns the projection of the datatype."
  [datatype]
  (.getProjection (coord-system datatype)))

(defmulti datatype-subset
  (fn [& args]
    (cond
     (every? #(isa? (class %) Range) (rest args)) :range
     (and (isa? (class (nth args 1)) java.util.Date)) :location)))

(defmethod datatype-subset :range [datatype range-t range-x range-y range-z]
  (let [service (. (:service datatype) subset range-t range-z range-y range-x)
        bounds (.. service getCoordinateSystem getLatLonBoundingBox)]
    (assoc datatype
      :service service
      :lat-min (.getLatMin bounds)
      :lat-max (.getLatMax bounds)
      :lat-size (.length range-y)
      :lon-min (.getLonMin bounds)
      :lon-max (.getLonMax bounds)
      :lon-size (.length range-x))))

(defmethod datatype-subset :location [datatype valid-time location & options]
  (let [options (apply hash-map options)
        point (. (projection datatype) latLonToProj (double (latitude location)) (double (longitude location))) 
        time-index (time-index datatype valid-time)
        width (or (:width options) 2)
        height (or (:height options) 2)
        bounds (LatLonRect. (LatLonPointImpl. (latitude location) (longitude location))
                            (LatLonPointImpl. (- (latitude location) (* (- height 1) (:lat-step datatype))) (+ (longitude location) (* (- width 1) (:lon-step datatype)))))
        [range-y range-x] (. (.getCoordinateSystem (:service datatype)) getRangesFromLatLonRect bounds)]
    [range-x range-y]
    (datatype-subset datatype (Range. time-index time-index) range-x range-y (Range. 0))))

(defn datatype-open?
  "Returns true if the datatype is open, else false."
  [datatype]
  (not (nil? (:service datatype))))

(defn location->index [datatype location]
  (let [[x y] (. (coord-system datatype) findXYindexFromLatLon (latitude location) (longitude location) nil)]
    {:x x :y y}))

(defn open-datatype
  "Open the NetCDF datatype."
  [datatype]
  (if-not (datatype-open? datatype)
    (let [datatype (assoc datatype :service (. (. GridDataset open (:dataset-uri datatype)) findGridDatatype (:variable datatype)))]
      (merge datatype (axis datatype)))))

(defmulti valid-times
  "Returns the valid times in the NetCDF datatype."
  class)

(defmethod valid-times GeoGrid [datatype]
  (.. (.getCoordinateSystem datatype) getTimeAxis1D getTimeDates))

(defmethod valid-times PersistentStructMap [datatype]
  (valid-times (or (:service datatype) (open-datatype datatype))))

(defn- read-data [datatype valid-time location]
  (let [datatype (:service datatype) dataset (GridAsPointDataset. [datatype])]
    (if (and (:altitude location) (. dataset hasVert datatype (:altitude location)))
      (. dataset readData datatype valid-time (:altitude location) (latitude location) (longitude location))
      (. dataset readData datatype valid-time (latitude location) (longitude location)))))

(defn- read-xy-data [datatype valid-time & [z-index]]  
  (. (:service datatype) readYXData (time-index datatype valid-time) (or z-index 0)))

(defmulti read-datapoint
  "Read the NetCDF datatype for the given time and location."
  (fn [datatype location & options]
    (class datatype)))

(defmethod read-datapoint PersistentStructMap [datatype location & options]
  (if location
    (let [options (apply hash-map options)
          valid-time (or (:valid-time options) (first (valid-times datatype)))
          data (read-data datatype valid-time location)]
      (struct-map record
        :actual-location (make-location (.lat data) (.lon data))
        :requested-location location
        :unit (.getUnitsString (:service datatype))
        :valid-time valid-time
        :value (or (and (.isNaN (.dataValue data)) (:nil options)) (.dataValue data))
        :variable (:variable datatype)))))

(defmethod read-datapoint Matrix [matrix location & options]
  (if location
    (let [datatype (:datatype (meta matrix))
          {:keys [x y]} (location->index datatype location)
          in-bounds (and (>= x 0) (>= y 0))
          value (if in-bounds (sel matrix y x) Double/NaN)]
      (struct-map record
        :actual-location (if in-bounds (. (coord-system datatype) getLatLon x y))
        :requested-location location
        :unit (.getUnitsString (:service datatype))
        :valid-time (:valid-time (meta matrix))
        :value (or (and (.isNaN value) (:nil (apply hash-map options))) value)
        :variable (:variable datatype)))))

(defn read-seq
  "Read the whole datatype as a sequence."
  [datatype & options]
  (let [options (apply hash-map options)
        valid-time (or (:valid-time options) (first (valid-times datatype)))]
    (with-meta (seq (. (read-xy-data datatype valid-time (:z-index options)) copyTo1DJavaArray))
      {:datatype datatype :valid-time valid-time})))

(defn read-matrix
  "Read the whole datatype as a matrix."
  [datatype & options]
  (let [sequence (apply read-seq datatype options)]
    (with-meta
      (matrix sequence (:lon-size (:datatype (meta sequence))))
      (meta sequence))))

;; (defn latitude->index [matrix latitude]
;;   (int (+ (/ longitude (:lon-step (meta matrix)))
;;           (:lon-max (meta matrix)))))

;; (defn longitude->index [matrix location]
;;   (int (+ (/ latitude (:lat-step (meta matrix)))
;;           (:lat-max (meta matrix)))))

;; (defn location->index [matrix location]
;;   {:x (longitude->index matrix (longitude location))
;;    :y (latitude->index matrix (latitude location))})

;; (meta *matrix*)

;; (matrix-index *matrix* (make-location 0 0))
;; (matrix-index *matrix* (make-location 78 -180))
;; (matrix-index *matrix* (make-location 78 180))
;; (matrix-index *matrix* (make-location 78 179))
;; (matrix-index *matrix* (make-location -78 180))
;; (matrix-index *matrix* (make-location -78 179))
;; (matrix-index *matrix* (make-location -78 -180))




;; (def *nww3* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))
;; (def *matrix* (read-matrix *nww3*))
;; (read-datapoint *nww3* (make-location 0 0))
;; (read-datapoint *matrix* (make-location 0 0))

;; (matrix-index *nww3* (make-location 78 180))
;; (matrix-index *nww3* (make-location 78 0))


;; (datatype-subset *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 5 :height 5)


;; (defn safe-ranges [datatype bounds]
;;   (let [coord-system (.. (:service datatype) getCoordinateSystem)
;;         projection (.getProjection coord-system)
;;         grid-bounds (.. (:service datatype) getCoordinateSystem getLatLonBoundingBox)]
;;     ;; (. projection latLonToProjBB bounds)
;;     ;; (. coord-system getRangesFromLatLonRect bounds)
;;     (. (. projection getDefaultMapAreaLL) intersect bounds)
;;     ))


;; (defn safe-bounds [grid-bounds bounds]
;;   (let [projection (.getProjection coord-system)
;;         grid-bounds (.. (:service datatype) getCoordinateSystem getLatLonBoundingBox)]
;;     ;; (. projection latLonToProjBB bounds)
;;     ;; (. coord-system getRangesFromLatLonRect bounds)
;;     (. (. projection getDefaultMapAreaLL) intersect bounds)
;;     ))

;; (safe-ranges *nww3* (LatLonRect. (LatLonPointImpl. 78 0) (LatLonPointImpl. 76 2.5)))
;; (.getLonMax (safe-ranges *nww3* (LatLonRect. (LatLonPointImpl. 79 -1) (LatLonPointImpl. 77 0))))

;; (println
;;  (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 79 0)))

;; (def *nww3* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))


;; (defn valid-times
;;   "Returns the valid times in the NetCDF datatype."
;;   [datatype]
;;   (if (datatype-open? datatype)
;;     (.. (.getCoordinateSystem (:service datatype)) getTimeAxis1D getTimeDates)
;;     (valid-times (open-datatype datatype))))

;; (def *nww3* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))
;; (println (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 5))


;; (save (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 15)
;;       "matrix.txt")

;; (defn read-matrix [datatype valid-time location & options]
;;   (let [sequence (read-seq datatype valid-time location width height)]
;;     (with-meta (matrix sequence (or width 2)) {})))

;; (println (read-seq *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 1))
;; (println (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0) :width 5))
;; (println (meta (read-matrix *nww3* (first (valid-times *nww3*)) (make-location 78 0))))

;; (defn matrix-read-datapoint [datatype valid-time location & [width height]]
;;   (let [width (or width 10) height (or height width)]    
;;     (matrix
;;      (for [latitude (reverse (range (latitude location) (+ (latitude location) height) (:step (latitude-axis datatype))))
;;            longitude (range (longitude location) (+ (longitude location) width) (:step (longitude-axis datatype)))]
;;        (:value (read-datapoint datatype valid-time (make-location latitude longitude))))
;;      width)))

;; (defn matrix-read-datapoint [datatype valid-time location & [width height]]
;;   (let [width (or width 10) height (or height width)]    
;;     (matrix
;;      (for [latitude (reverse (sample-latitude (latitude location) height (:step (latitude-axis datatype))))
;;            longitude (sample-longitude (longitude location) width (:step (longitude-axis datatype)))]
;;        (:value (read-datapoint datatype valid-time (make-location latitude longitude))))
;;      width)))


;; (println (matrix-read-datapoint *nww3* (first (valid-times *nww3*)) (make-location 78 0) 7))
;; (println (matrix-interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 78 0) 7))
;; (:value (interpolate-bilinear *nww3* (first (valid-times *nww3*)) (make-location 77 0)))

;; (defn matrix-read [datatype valid-time & options]
;;   (let [options (apply hash-map options)
;;         read-fn (or (:read-fn options read-datapoint))]))

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

;; (:value (read-datapoint *akw* (first (valid-times *akw*)) (make-location 78 0)))
;; (:value (read-datapoint *akw* (first (valid-times *akw*)) (make-location 77 0)))

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
;;   (let [lat-step (:step (latitude-axis datatype)) lon-step (:step (longitude-axis datatype))]
;;     (for [latitude (reverse (range (latitude location) (+ (latitude location) (* num lat-step)) lat-step))
;;           longitude (range (longitude location) (+ (longitude location) (* num lon-step)) lon-step)]
;;       (make-location latitude longitude))))

;; ;; (defn location-range [lat1 lon1 lat2 lon2]
;;   (let [lat-step (:step (latitude-axis datatype)) lon-step (:step (longitude-axis datatype))]
;;     (for [latitude (reverse (range (latitude location) (+ (latitude location) (* num lat-step)) lat-step))
;;           longitude (range (longitude location) (+ (longitude location) (* num lon-step)) lon-step)]
;;       (make-location latitude longitude))))


  
;;   (map #(read-datapoint datatype valid-time %) (location-rect datatype location 2))
;; (location-rect *akw* {:latitude 0 :longitude 0} 3)


 ;; (map :value (read-samples *akw* (first (valid-times *akw*)) {:latitude 76 :longitude 0}))



;; (defn read-range [datatype valid-time lat-range lon-range]
;;   (for [ latitude (reverse lat-range) longitude lon-range]
;;     (read-datapointyyyyyyyyyyyy datatype valid-time {:latitude latitude :longitude longitude})))

;; (into-array (map :value (read-range *akw* (first (valid-times *akw*)) (range 75 79 1) (range 0 5.0 1.25))))

;; (reverse (map :value (read-range *akw* (first (valid-times *akw*)) (range 75 79 1) (range 0 5.0 1.25))))

;; (println
;;  (matrix
;;   (map :value (read-range *akw* (first (valid-times *akw*)) (range 75 79 1) (range 0 5.0 1.25)))
;;   4))






;; (defn sample-locations [datatype location num]
;;   (let [location (central-sample-location datatype location)
;;         lat-step (:step (latitude-axis datatype))
;;         lon-step (:step (longitude-axis datatype))]
;;     (location-range
;;      (make-location (- (latitude location) (* lat-step num)) (- (longitude location) (* lon-step num)))
;;      (make-location (+ (latitude location) (* lat-step num)) (+ (longitude location) (* lon-step num)))
;;      :lat-step lat-step :lon-step lon-step)))

;; (defn sample-locations [datatype location width height]
;;   (let [location (central-sample-location datatype location)
;;         lat-step (:step (latitude-axis datatype))
;;         lon-step (:step (longitude-axis datatype))]
;;     (for [
;;           latitude (range (- (latitude location) (* (/ width 2) lat-step)) (+ (latitude location) (* (/ width 2) lat-step)) lat-step)
;;           longitude (range (- (longitude location) (* (/ height 2) lon-step)) (+ (longitude location) (* (/ height 2) lon-step)) lon-step)
;; ]
;;       [latitude longitude]
;;       )))

