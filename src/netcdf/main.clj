(ns netcdf.main
  (:gen-class)
  (:require [netcdf.dods :as dods]
            [netcdf.model :as models])
  (:use [commandline.core :only (with-commandline *options*)]
        [clojure.java.io :only (reader)]
        [clojure.string :only (join)]
        netcdf.forecast))

(defn download
  "Download the weather forecast at reference time to directory."
  [reference-time directory]
  (download-forecast surf-forecast :directory directory :reference-time reference-time))

(defn dump
  "Read locations from standard input and dump their forecast at
  reference time to standard out."
  [reference-time]
  (let [keys (map (comp keyword :name) (keys (:variables surf-forecast)))]
    (doseq [location (line-seq (reader *in*))]
      (dump-forecast surf-forecast location :reference-time reference-time))))

(defn print-help
  "Print help about the command."
  [] (commandline.core/print-help "netcdf [OPTION,...] COMMAND"))

(defn print-reference-times
  "Print the latest reference times of all models."
  [] (doseq [model (sort-by :name (vals @models/*models*))]
       (println (join "\t" [(:name model) (dods/latest-reference-time model) (:description model)]))))

(defn -main [& args]
  (with-commandline [args]
    [[h help "Print this help."]
     [d directory "The DIRECTORY where the NetCDF files are stored (default: ~/.netcdf)." :string "DIRECTORY"]
     [r reference-time "The reference TIME of the model." :time "TIME"]]
    (let [[command arguments] arguments]
      (cond
       (= "download" command)
       (download reference-time directory)
       (= "dump" command)
       (dump reference-time)
       (= "reference-times" command)
       (print-reference-times)
       :else (print-help)))))
