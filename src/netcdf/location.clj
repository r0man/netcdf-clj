(ns netcdf.location
  (:refer-clojure :exclude (replace))
  (:require [geocoder.core :as geocoder])
  (:import (ucar.unidata.geoloc Bearing LatLonPointImpl LatLonPoint))
  (:use [clojure.string :only (split replace trim)]))

(defprotocol ILocation
  (to-location [object] "Convert object into a location."))

(defn parse-double [string]
  (try (if (number? string)
         string
         (Double/parseDouble (str string)))
       (catch NumberFormatException exception nil)))

(defn parse-dms [string & {:keys [junk-allowed]}]
  (if (number? string)
    string
   (try
     (let [[degrees minutes seconds] (map parse-double (split (replace (replace (trim (or string "")) #"^-" "") #"[NSEW]$" "") #"[^0-9.,]+"))]
       (* (if (re-matches #"(?i)(^-).*|(.*[WS])$" (or string "")) -1.0 1.0)
          (cond
           (and degrees minutes seconds)
           (+ (/ degrees 1) (/ minutes 60) (/ seconds 360))
           (and degrees minutes)
           (+ (/ degrees 1) (/ minutes 60))
           :else degrees)))
     (catch Exception e
       (if-not junk-allowed
         (throw (IllegalArgumentException. (format "Can't parse: %s." string))))))))

(defn parse-latitude [str & {:keys [junk-allowed]}]
  (parse-dms str :junk-allowed junk-allowed))

(defn parse-longitude [str & {:keys [junk-allowed]}]
  (parse-dms str :junk-allowed junk-allowed))

(defn latitude [location]
  (cond
   (isa? (class location) LatLonPointImpl) (.getLatitude location)
   :else (:latitude location)))

(defn longitude [location]
  (cond
   (isa? (class location) LatLonPointImpl) (.getLongitude location)
   :else (:longitude location)))

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

(defn location? [location]
  (and location (latitude location) (longitude location) location))

(defn location->array [location]
  [(latitude location) (longitude location)])

(defn location->map [location]
  {:latitude (latitude location) :longitude (longitude location)})

(defn parse-location [lat-lon-str & {:keys [junk-allowed]}]
  (if (location? lat-lon-str)
    lat-lon-str
    (try
      (let [[latitude longitude] (split lat-lon-str #",|;|\t")]
        (make-location (parse-latitude latitude) (parse-longitude longitude)))
      (catch Exception e
        (if-not junk-allowed
          (throw (IllegalArgumentException. (format "Can't parse: %s." lat-lon-str))))))))

(defn in-bounding-box?
  "Returns true if the location is in the bounding box, else false."
  [bounding-box location]
  (. bounding-box contains (latitude location) (longitude location)))

;; Missing in NETCDF 4.2?
;; (defn destination-point [location azimuth-in-deg distance-in-km]
;;   (if-let [point (. Bearing findPoint (latitude location) (longitude location) azimuth-in-deg distance-in-km nil)]
;;     (make-location (.getLatitude point) (.getLongitude point))))

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

(defmethod print-method LatLonPoint
  [location writer]
  (print-method
   {:latitude (latitude location)
    :longitude (longitude location)} writer))

(defn resolve-location
  "Resolves a location by parsing or geocoding."
  [location]
  (or (parse-location location :junk-allowed true)
      (if-let [location (:location (first (geocoder/geocode location)))]
        (make-location (:latitude location) (:longitude location)))))

(defn to-point [location]
  (if location
    (LatLonPointImpl. (latitude location) (longitude location))))

(extend-type LatLonPoint
  ILocation
  (to-location [object]
    object))

(extend-type clojure.lang.IPersistentMap
  ILocation
  (to-location [map]
    (LatLonPointImpl. (:latitude map) (:longitude map))))
