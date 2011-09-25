(ns netcdf.main
  (:gen-class)
  (:require [netcdf.dods :as dods]
            [netcdf.model :as models])
  (:use [commandline.core :only (with-commandline *options*)]
        [clojure.string :only (join)]))

(defn print-help []
  (commandline.core/print-help "netcdf [OPTION,...] COMMAND"))

(defn print-reference-times []
  (doseq [model (vals @models/*models*)]
    (println (join "\t" [ (dods/latest-reference-time model) (:name model) (:description model)]))))

(defn -main [& args]
  (with-commandline [args]
    [[h help "Print this help."]
     [r reference-time "The reference TIME of the model." :time "TIME"]]
    (let [[command arguments] arguments]
      (cond
       (or (= "help" command) help)
       (print-help)
       (= "reference-times" command)
       (print-reference-times)
       :else (print-help)))))
