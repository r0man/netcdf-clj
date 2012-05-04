(ns netcdf.time
  (:import org.joda.time.DateTime org.joda.time.IllegalFieldValueException)
  (:use [clj-time.core :only (date-time)]
        [clj-time.coerce :only (to-date-time)]
        [clj-time.format :only (formatters parse unparse)]))

(def ^:dynamic *date-formatter* :date)
(def ^:dynamic *time-formatter* :date-time-no-ms)

(def path-pattern #".*(\d{4})/(\d{2})/(\d{2})/.*(\d{2})(\d{2})(\d{2}).*")

(defn date-time?
  "Returns true if arg is a DateTime object, otherwise false."
  [arg] (instance? DateTime arg))

(defn format-time
  "Format the object with the default or the given time formatter."
  [object & [formatter]]
  (if object
    (unparse (or formatter (formatters *time-formatter*))
             (to-date-time object))))

(defn parse-fragment
  "Parse the time from a path/url fragment."
  [path]
  (if-let [[_ year month day hour minute second] (re-find path-pattern (str path))]
    (try
      (date-time
       (Integer/parseInt year)
       (Integer/parseInt month)
       (Integer/parseInt day)
       (Integer/parseInt hour)
       (Integer/parseInt minute)
       (Integer/parseInt second))
      (catch IllegalFieldValueException _ nil))))
