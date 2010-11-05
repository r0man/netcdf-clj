(ns netcdf.repository
  (:require [netcdf.dods :as dods])
  (:use [clojure.contrib.def :only (defvar)]))

(defvar *repositories* (ref {})
  "The map of repositories.")

(defrecord Repository [name url description])

(defn find-repository-by-name
  "Lookup a registered repository by name."
  [name] (get @*repositories* (keyword name)))

(defn make-repository [name url & [description]]
  (Repository. name url description))

(defn register-repository
  "Register the repository."
  [repository]
  (dosync (ref-set *repositories* (assoc @*repositories* (keyword (:name repository)) repository)))
  repository)

(defn repository?
  "Returns true if arg is a repository, otherwise false."
  [arg] (isa? (class arg) Repository))

(defn unregister-repository
  "Unregister the repository."
  [repository]
  (dosync (ref-set *repositories* (dissoc @*repositories* (keyword (:name repository)))))
  repository)

(defn global-forecast-system-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map find-repository-by-name ["gfs-hd"])))

(defn wave-watch-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map find-repository-by-name ["akw" "enp" "nah" "nph" "nww3" "wna"])))

(defn download-variables [repositories variables]  
  (doseq [repository repositories]
    (println (str "* " (:description repository)))
    (println (str "  Url: " (:url repository)))
    (doseq [variable variables]
      (time
       (do
         (print (str "  - " variable " "))
         (throw (Exception. "FIXME"))
         ;; (println (str (download-variable repository variable)))
         (print "    "))))))

(defn download-global-forecast-system [& variables]
  (println "Downloading the Global Forecast System Model ...")
  (download-variables (global-forecast-system-repositories) (or variables ["tmpsfc"])))

(defn download-wave-watch [& variables]
  (println "Downloading the Wave Watch III Model ...")
  (download-variables (wave-watch-repositories) (or variables ["htsgwsfc"])))

(defn reference-times
  "Returns all reference times for repository."
  [repository] (map :reference-time (dods/find-datasets-by-url (:url repository))))

(defn latest-reference-time
  "Returns the latest reference times for repository."
  [repository] (last (sort (reference-times repository))))

(defmacro defrepo
  "Define and register the repository."
  [name url & [description]]
  (register-repository (make-repository name url description)))

(defrepo "akw"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/akw"
  "Regional Alaska Waters Wave Model")

(defrepo "enp"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/enp"
  "Regional Eastern North Pacific Wave Model")

(defrepo "nah"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/nah"
  "Regional Atlantic Hurricane Wave Model")

(defrepo "nph"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/nph"
  "Regional North Pacific Hurricane Wave Model")

(defrepo "nww3"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3"
  "Global NOAA Wave Watch III Model")

(defrepo "wna"
  "http://nomads.ncep.noaa.gov:9090/dods/wave/wna"
  "Regional Western North Atlantic Wave Model")

(defrepo "gfs-hd"
  "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  "Global Forecast Model")
