(ns leiningen.netcdf
  (:use [clojure.string :only (blank? split)]
        clojure.contrib.command-line
        netcdf.model
        netcdf.variable))

(defn- parse-models [models]
  (if-not (blank? models)
    (map lookup-model (split models #","))
    (vals @*models*)))

(defn- parse-variables [model variables]
  (if-not (blank? variables)
    (let [variables (set (split variables #","))]
      (filter #(contains? variables (:name %)) (:variables model)))
    (:variables model)))

(defn download [& args]
  (with-command-line
    args
    "Usage: netcdf download [OPTIONS]"
    [[models m "The list of models to download (Example: akw,nww3)."]
     [variables v "The list of variables to download (Example: htsgwsfc,windsfc)."]
     [reference-time r "The reference time (Example: 2011-03-13T12:00:00Z)."]]
    (doseq [model (parse-models models) variable (parse-variables model variables)]
      (download-variable model variable :reference-time reference-time))))

(defn netcdf
  "NetCDF tasks."
  [project & [command & args]]
  (condp = command
    "download" (apply download args)))
