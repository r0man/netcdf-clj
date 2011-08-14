(ns netcdf.model
  (:import java.io.File org.joda.time.Interval)
  (:require [netcdf.dods :as dods]
            [netcdf.geo-grid :as grid])
  (:use [clojure.contrib.def :only (defvar)]
        [clojure.string :only (join)]
        [clojure.contrib.duck-streams :only (with-out-writer)]
        [clojure.contrib.json :only (read-json json-str)]
        [clj-time.core :only (before? after? now in-secs interval date-time year month day hour within? minus plus minutes days)]
        [netcdf.dataset :only (copy-dataset find-geo-grid open-grid-dataset)]
        netcdf.bounding-box
        netcdf.utils
        netcdf.variable
        netcdf.repository
        netcdf.dods
        netcdf.time
        clojure.contrib.logging
        clj-time.format))

(defrecord Model [name description dods variables resolution])

(defvar *models* (ref {})
  "The model cache.")

(defvar *variables* (ref {})
  "The variable cache.")

(defn make-model [& {:keys [name description dods variables resolution]}]
  (Model. name description dods variables resolution))

(defn model?
  "Returns true if arg is a model, otherwise false."
  [arg] (isa? (class arg) Model))

(defn register-model [model]
  (dosync (ref-set *models* (assoc @*models* (keyword (:name model)) model))))

(defmacro defmodel
  "Define and register the model."
  [name description & {:keys [dods variables resolution]}]
  (let [name# name description# description dods# dods variables# variables resolution# resolution]
    `(do (defvar ~name#
           (make-model
            :description ~description#
            :name ~(str name#)
            :dods ~dods#
            :variables (set ~variables#)
            :resolution ~resolution#)
           ~description#)
         (register-model ~name#))))

(defn model
  "Lookup model and it's variables by name."
  [name & [variables]]
  (if-let [model (get @*models* (keyword name))]
    (assoc model
      :variables
      (if-not variables
        (:variables model)
        (remove #(not (contains? (set variables) (:name %))) (:variables model))))))

(defn find-dataset [model & [reference-time]]
  (first (dods/find-datasets-by-url-and-reference-time
          (:dods model) (or reference-time (latest-reference-time model)))))

(defn download-model [model & options]
  (let [variables (remove nil? (map #(apply download-variable model % options) (:variables model)))]
    (assoc model
      :interval (Interval.
                 (.getStartMillis (:interval (first variables)))
                 (.getEndMillis (:interval (last variables))))
      :reference-time (:reference-time (first variables))
      :size (reduce + (remove nil? (map :size variables)))
      :variables variables)))

(defn download-models [models & [reference-time]]
  (doall (map #(download-model % :reference-time reference-time) models)))

(defn meta-data [model reference-time]
  (if-let [dataset (first (dods/find-datasets-by-url-and-reference-time (:dods model) reference-time))]
    (with-open [netcdf (open-grid-dataset (:dods dataset))]
      (assoc dataset
        :name (:name model)
        :bounds (.getBoundingBox netcdf)
        :description (:description model)))))

(defn model-info [model]
  (let [latest-reference-time (latest-reference-time model)]
    (assoc model :bounding-box (to-map (:bounds (meta-data model latest-reference-time))))))

(defn write-model-info [filename models]
  (spit filename (prn-str (map model-info models))))

(defn read-model-info [filename]
  (read-string (slurp filename)))

(defn read-model [model location & [reference-time]]
  (let [reference-time (or reference-time (latest-reference-time model))]
    (flatten (map #(read-variable model % location reference-time) (:variables model)))))

(defn dump-model [model filename pois & [reference-time separator]]
  (with-out-writer filename
    (doseq [meassure (read-model model pois reference-time)]
      (println
       (join
        (or separator "\t")
        [(:id model)
         (:id (:variable meassure))
         (:id meassure)
         (:latitude (:location meassure))
         (:longitude (:location meassure))
         (format-time (:reference-time meassure))
         (format-time (:valid-time meassure))
         (:value meassure)
         (:unit (:variable meassure))])))))

(defn sort-by-resolution
  "Sort the models by their resolution."
  [models] (sort-by #(* (:latitude (:resolution %)) (:longitude (:resolution %))) models))

(defmodel akw
  "Regional Alaska Waters Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
  :variables wave-watch-variables
  :resolution {:latitude 0.25 :longitude 0.5})

(defmodel enp
  "Regional Eastern North Pacific Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/enp"
  :variables wave-watch-variables
  :resolution {:latitude 0.25 :longitude 0.25})

(defmodel gfs-hd
  "Global Forecast Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  :variables gfs-variables
  :resolution {:latitude 0.5 :longitude 0.5})

(defmodel nah
  "Regional Atlantic Hurricane Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nah"
  :variables wave-watch-variables
  :resolution {:latitude 0.5 :longitude 0.25})

(defmodel nph
  "Regional North Pacific Hurricane Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nph"
  :variables wave-watch-variables
  :resolution {:latitude 0.25 :longitude 0.5})

(defmodel nww3
  "Global NOAA Wave Watch III Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"
  :variables wave-watch-variables
  :resolution {:latitude 1.0 :longitude 1.25})

(defmodel wna
  "Regional Western North Atlantic Wave Model"
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/wna"
  :variables wave-watch-variables
  :resolution {:latitude 0.5 :longitude 0.5})

(defvar gfs-models
  [gfs-hd] "The models of the Global Forecast System.")

(defvar wave-watch-models
  [nww3 akw enp nah nph wna]
  "The the Wave Watch III models.")

(defn download-gfs [& [reference-time]]
  (download-models gfs-models reference-time))

(defn download-wave-watch [& [reference-time]]
  (download-models wave-watch-models reference-time))
