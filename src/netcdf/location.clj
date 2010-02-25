(ns netcdf.location
  (:import ucar.unidata.geoloc.Bearing)
  (:use [clojure.contrib.str-utils :only (re-split)]))

(defstruct location :latitude :longitude :altitude)

(defn parse-double [str]
  (try (Double/parseDouble str)
       (catch NumberFormatException exception nil)))

(defn make-location
  ([latitude longitude]
     "Make a location with the given latitude and longitude."
     (make-location latitude longitude nil))
  ([latitude longitude altitude]
     "Make a location with the given latitude, longitude and altitude."
     (struct location latitude longitude altitude)))

(defn location? [location]
  (and (:latitude location) (:longitude location) location))

(defn location->array [location]
  [(:latitude location) (:longitude location)])

(defn parse-latitude [str]
  (parse-double str))

(defn parse-longitude [str]
  (parse-double str))

(defn parse-location [lat-lon-str]
  (let [[latitude longitude] (re-split #",|;|\s+" lat-lon-str)]
    (make-location (parse-latitude latitude) (parse-longitude longitude))))

(defn in-bounding-box?
  "Returns true if the location is in the bounding box, else false."
  [bounding-box location]  
  (. bounding-box contains (:latitude location) (:longitude location)))

(defn destination-point [location azimuth-in-deg distance-in-km]
  (if-let [point (. Bearing findPoint (:latitude location) (:longitude location) azimuth-in-deg distance-in-km nil)]
    (make-location (.getLatitude point) (.getLongitude point))))

(defn distance [from-location to-location]
  (if-let [bearing (. Bearing calculateBearing (:latitude from-location) (:longitude from-location) (:latitude to-location) (:longitude to-location) nil)]
    (.getDistance bearing)))

(defn latitude-distance [from-location to-location]
  (- (:latitude to-location) (:latitude from-location)))

(defn longitude-distance [from-location to-location]
  (- (:longitude to-location) (:longitude from-location)))

(defn latitude-range [latitude-1 latitude-2 & [step]]
  (range (min latitude-1 latitude-2) (max latitude-1 latitude-2) (or step 1)))

(defn longitude-range [longitude-1 longitude-2 & [step]]
  (range (min longitude-1 longitude-2) (max longitude-1 longitude-2) (or step 1)))

(defn location-range [location-1 location-2 & options]
  (let [options (apply hash-map options)]
    (for [latitude (latitude-range (:latitude location-1) (:latitude location-2) (:step-lat options ))
          longitude (longitude-range (:longitude location-1) (:longitude location-2) (:step-lon options ))]
      (make-location latitude longitude))))

(defn location-rect [location & options]
  (let [options (apply hash-map options)
        width (or (:width options) 1)
        height (or (:height options) width)
        lat-step (or (:lat-step options) (:lon-step options) 1)
        lon-step (or (:lon-step options) lat-step)]
    (for [latitude (reverse (range (- (:latitude location) (* (- height 2) lat-step) lat-step) (+ (:latitude location) lat-step) lat-step))
          longitude (range (:longitude location) (+ (:longitude location) (* (- width 1) lon-step) lon-step) lon-step)]
      (make-location latitude longitude))))

(defn north? [source target]
  (> (:latitude source) (:latitude target)))

(defn east? [source target]
  (> (:longitude source) (:longitude target)))

(defn south? [source target]
  (< (:latitude source) (:latitude target)))

(defn west? [source target]
  (< (:longitude source) (:longitude target)))

(defn north-east? [source target]
  (and (north? source target) (east? source target)))

(defn south-east? [source target]
  (and (south? source target) (east? source target)))

(defn south-west? [source target]
  (and (south? source target) (west? source target)))

(defn north-west? [source target]
  (and (north? source target) (west? source target)))


