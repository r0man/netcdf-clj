(ns netcdf.map
  (:import javax.swing.ImageIcon)
  (:use [clojure.contrib.str-utils2 :only (join)])
  (:require [clojure.contrib.http.agent :as agent]))

(def *base-url* "http://maps.google.com/maps/api/staticmap")
(def *options* {:center {:latitude 0 :longitude 0} :width 300 :height 200 :maptype "roadmap" :sensor false :zoom 1})

(defn options->params [options]
  (join "&" (map #(str (name (first %)) "=" (last %)) options)))

(defn parse-options [options]
  (let [{:keys [latitude longitude]} (:center options)]
    (dissoc
     (assoc (merge *options* options)
       :center (str latitude "," longitude)   
       :size (str (or (:width options) (:width *options*)) "x" (or (:height options) (:height *options*))))
     :center :width :height)))

(defn static-map-url [center & options]
  (let [options (apply hash-map options)]
    (str *base-url* "?" (options->params (parse-options (assoc options :center center))))))

(defn static-map-image [center & options]
  (.getImage (ImageIcon. (agent/bytes (agent/http-agent (apply static-map-url center options))))))

