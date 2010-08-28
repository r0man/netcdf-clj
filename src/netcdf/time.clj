(ns netcdf.time
  (:use [clojure.contrib.def :only (defvar)]
        [clj-time.coerce :only (from-date to-date)]
        [clj-time.format :only (unparse parse formatters)]))

(defvar *formatter* :basic-date-time-no-ms
  "The default formatter for time formatting.")

(defn parse-time [string & {:keys [formatter]}]
  (parse (formatters (or formatter *formatter*)) string))

(defn format-time [date-time & {:keys [formatter]}]
  (unparse (formatters (or formatter *formatter*)) date-time))
