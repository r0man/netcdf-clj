(ns netcdf.location
  (:import (ucar.unidata.geoloc Bearing LatLonPointImpl))
  (:use [clojure.contrib.str-utils :only (re-split)]))

(defn parse-double [string]
  (try (if (number? string)
         string
         (Double/parseDouble (str string)))
       (catch NumberFormatException exception nil)))

(defn parse-latitude [str]
  (parse-double str))

(defn parse-longitude [str]
  (parse-double str))

(defmulti make-location
  "Make a location with the given latitude and longitude."  
  (fn [& args]
    (map class args)))

(defmethod make-location [clojure.lang.PersistentArrayMap] [location]
  (make-location (latitude location) (longitude location)))

(defmethod make-location [String String] [latitude longitude]
  (LatLonPointImpl. (parse-latitude latitude) (parse-longitude longitude)))

(defmethod make-location :default [latitude longitude]
  (LatLonPointImpl. latitude longitude))

(defn latitude [location]
  (cond
   (isa? (class location) LatLonPointImpl) (.getLatitude location)
   :else (:latitude location)))

(defn longitude [location]
  (cond
   (isa? (class location) LatLonPointImpl) (.getLongitude location)
   :else (:longitude location)))

(defn location? [location]
  (and location (latitude location) (longitude location) location))

(defn location->array [location]
  [(latitude location) (longitude location)])

(defn location->map [location]
  {:latitude (latitude location) :longitude (longitude location)})

(defn parse-location [lat-lon-str]
  (let [[latitude longitude] (re-split #",|;|\s+" lat-lon-str)]
    (make-location (parse-latitude latitude) (parse-longitude longitude))))

(defn in-bounding-box?
  "Returns true if the location is in the bounding box, else false."
  [bounding-box location]  
  (. bounding-box contains (latitude location) (longitude location)))

(defn destination-point [location azimuth-in-deg distance-in-km]
  (if-let [point (. Bearing findPoint (latitude location) (longitude location) azimuth-in-deg distance-in-km nil)]
    (make-location (.getLatitude point) (.getLongitude point))))

(defn distance [from-location to-location]
  (if-let [bearing (. Bearing calculateBearing (latitude from-location) (longitude from-location) (latitude to-location) (longitude to-location) nil)]
    (.getDistance bearing)))

(defn latitude-distance [from-location to-location]
  (- (latitude to-location) (latitude from-location)))

(defn longitude-distance [from-location to-location]
  (- (longitude to-location) (longitude from-location)))

(defn latitude-range [latitude height & [step]]
  (range
   (- latitude (* (- height 1) (or step 1)))
   (+ latitude (or step 1))
   (or step 1)))

(defn longitude-range [longitude width & [step]]
  (range
   longitude
   (+ longitude (* (- width 0) (or step 1)))
   (or step 1)))

(defn location-range [location-1 location-2 & options]
  (let [options (apply hash-map options)]
    (for [latitude (latitude-range (latitude location-1) (latitude location-2) (:lat-step options ))
          longitude (longitude-range (longitude location-1) (longitude location-2) (:lon-step options ))]
      (make-location latitude longitude))))

(defn location-rect [location & options]
  (let [options (apply hash-map options)
        width (or (:width options) 1)
        height (or (:height options) width)
        lat-step (or (:lat-step options) (:lon-step options) 1)
        lon-step (or (:lon-step options) lat-step)]
    (for [latitude (reverse (range (- (latitude location) (* (- height 2) lat-step) lat-step) (+ (latitude location) lat-step) lat-step))
          longitude (range (longitude location) (+ (longitude location) (* (- width 1) lon-step) lon-step) lon-step)]
      (make-location latitude longitude))))

(defn north? [source target]
  (> (latitude source) (latitude target)))

(defn east? [source target]
  (> (longitude source) (longitude target)))

(defn south? [source target]
  (< (latitude source) (latitude target)))

(defn west? [source target]
  (< (longitude source) (longitude target)))

(defn north-east? [source target]
  (and (north? source target) (east? source target)))

(defn south-east? [source target]
  (and (south? source target) (east? source target)))

(defn south-west? [source target]
  (and (south? source target) (west? source target)))

(defn north-west? [source target]
  (and (north? source target) (west? source target)))


