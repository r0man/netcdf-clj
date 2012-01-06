(ns netcdf.main
  (:gen-class)
  (:require [netcdf.dods :as dods]
            [netcdf.forecast :as forecasts]
            [netcdf.model :as models])
  (:use [commandline.core :only (with-commandline *options*)]
        [clojure.string :only (join)]))

(defn download [reference-time directory]
  (forecasts/download-forecast forecasts/surf-forecast :directory directory :reference-time reference-time))

(defn print-help []
  (commandline.core/print-help "netcdf [OPTION,...] COMMAND"))

(defn print-reference-times []
  (doseq [model (sort-by :name (vals @models/*models*))]
    (println (join "\t" [(:name model) (dods/latest-reference-time model) (:description model)]))))

(defn -main [& args]
  (with-commandline [args]
    [[h help "Print this help."]
     [d directory "The DIRECTORY where the NetCDF files are stored (default: ~/.netcdf)." :string "DIRECTORY"]
     [r reference-time "The reference TIME of the model." :time "TIME"]]
    (let [[command arguments] arguments]
      (cond
       (or (= "help" command) help)
       (print-help)
       (= "download" command)
       (download reference-time directory)
       (= "reference-times" command)
       (print-reference-times)
       :else (print-help)))))
