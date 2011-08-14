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

(defrecord Model [name description bounding-box dods resolution variables])

(defvar *models* (atom {})
  "The model cache.")

(defvar *variables* (atom {})
  "The variable cache.")

(defn make-model [& {:keys [name description bounding-box dods resolution variables]}]
  (Model. name description bounding-box dods resolution (set variables)))

(defn model?
  "Returns true if arg is a model, otherwise false."
  [arg] (isa? (class arg) Model))

(defn lookup-model
  "Lookup a model by name."
  [name] (get @*models* (keyword (clojure.core/name name))))

(defn lookup-variable
  "Lookup a variable by name."
  [name] (get @*variables* (keyword (clojure.core/name name))))

(defn register-model
  "Register a model by name."
  [model] (swap! *models* assoc (keyword (:name model)) model))

(defn register-variable
  "Register a variable by name."
  [model variable]
  (swap! *variables* assoc (keyword (:name variable))
         (-> variable
             (assoc :models
               (conj (or (:models (lookup-variable (:name variable))) #{}) model)))))

(defn register-variables
  "Register the variables by name."
  [model variables]
  (doall (map #(register-variable model %) variables)))

(defmacro defmodel
  "Define and register the model."
  [name description & {:keys [bounding-box dods resolution variables]}]
  (let [name# name description# description bounding-box# bounding-box dods# dods resolution# resolution variables# variables]
    `(do (defvar ~name#
           (make-model
            :bounding-box ~bounding-box#
            :description ~description#
            :dods ~dods#
            :name ~(str name#)
            :resolution ~resolution#
            :variables ~variables#)
           ~description#)
         (register-model ~name#)
         (register-variables ~name# ~variables#))))

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

(defn best-model-for-location [models location]
  "Returns the model with the highest resolution that covers the location."
  [location] (first (filter #(.contains (:bounding-box %) location) (sort-by-resolution models))))

(defmodel akw
  "Regional Alaska Waters Wave Model"
  :bounding-box (make-bounding-box 44.75 159.5 75.25 -123.5)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
  :resolution {:latitude 0.25 :longitude 0.5}
  :variables wave-watch-variables)

(defmodel enp
  "Regional Eastern North Pacific Wave Model"
  :bounding-box (make-bounding-box 4.75 -170.25 60.5 -77.25)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/enp"
  :resolution {:latitude 0.25 :longitude 0.25}
  :variables wave-watch-variables)

(defmodel gfs-hd
  "Global Forecast Model"
  :bounding-box (make-bounding-box -90.0 0.0 90.0 -0.5)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  :resolution {:latitude 0.5 :longitude 0.5}
  :variables gfs-variables)

(defmodel nah
  "Regional Atlantic Hurricane Wave Model"
  :bounding-box (make-bounding-box -0.25 -98.25 50.25 -29.75)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nah"
  :resolution {:latitude 0.5 :longitude 0.25}
  :variables wave-watch-variables)

(defmodel nph
  "Regional North Pacific Hurricane Wave Model"
  :bounding-box (make-bounding-box 4.75 -170.25 60.5 -77.25)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nph"
  :resolution {:latitude 0.25 :longitude 0.5}
  :variables wave-watch-variables)

(defmodel nww3
  "Global NOAA Wave Watch III Model"
  :bounding-box (make-bounding-box -78.0 0.0 78.0 -1.25)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"
  :resolution {:latitude 1.0 :longitude 1.25}
  :variables wave-watch-variables)

(defmodel wna
  "Regional Western North Atlantic Wave Model"
  :bounding-box (make-bounding-box -0.25 -98.25 50.25 -29.75)
  :dods "http://nomads.ncep.noaa.gov:9090/dods/wave/wna"
  :resolution {:latitude 0.5 :longitude 0.5}
  :variables wave-watch-variables)

(defvar gfs-models
  [gfs-hd] "The models of the Global Forecast System.")

(defvar wave-watch-models
  [nww3 akw enp nah nph wna]
  "The the Wave Watch III models.")

(defn download-gfs [& [reference-time]]
  (download-models gfs-models reference-time))

(defn download-wave-watch [& [reference-time]]
  (download-models wave-watch-models reference-time))
