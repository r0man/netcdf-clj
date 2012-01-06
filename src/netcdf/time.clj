(ns netcdf.time
  (:import java.util.Date
           (org.joda.time DateTime DateTimeZone IllegalFieldValueException)
           (org.joda.time.format ISODateTimeFormat))
  (:use [clj-time.format :only (formatters parse unparse)]
        [clj-time.core :only (date-time now)]))

(def ^:dynamic *date-formatter* :date)
(def ^:dynamic *time-formatter* :date-time-no-ms)

(defn- parse-date-time [string formatter]
  (parse (formatters formatter) string))

(defprotocol TimeProtocol
  (to-ms [this] "Returns the time in milliseconds."))

(extend-type java.lang.Number
  TimeProtocol
  (to-ms [number] number))

(extend-type java.lang.String
  TimeProtocol
  (to-ms
    [string]
    (try
      (to-ms (parse-date-time string *time-formatter*))
      (catch IllegalArgumentException _
        (to-ms (parse-date-time string *date-formatter*))))))

(extend-type java.util.Calendar
  TimeProtocol
  (to-ms [calendar] (to-ms (.getTime calendar))))

(extend-type java.util.Date
  TimeProtocol
  (to-ms [date] (.getTime date)))

(extend-type org.joda.time.DateTime
  TimeProtocol
  (to-ms [date-time] (.getMillis date-time)))

(extend-type nil
  TimeProtocol
  (to-ms [_] nil))

(defn date-time?
  "Returns true if arg is a DateTime object, otherwise false."
  [arg] (instance? DateTime arg))

(defn to-calendar
  "Convert the object to a java.util.Calendar."
  [object]
  (doto (java.util.Calendar/getInstance)
    (.setTimeInMillis (to-ms object))))

(defn to-date
  "Convert the object to a java.sql.Date."
  [object] (if object (java.sql.Date. (to-ms object))))

(defn to-date-time
  "Convert the object to a org.joda.time.DateTime."
  [object] (if object (DateTime. (long (to-ms object)) (DateTimeZone/UTC))))

(defn format-date
  "Format the object with the default or the given date formatter."
  [object & [formatter]]
  (if object
    (unparse (or formatter (formatters *date-formatter*))
             (to-date-time object))))

(defn format-time
  "Format the object with the default or the given time formatter."
  [object & [formatter]]
  (if object
    (unparse (or formatter (formatters *time-formatter*))
             (to-date-time object))))

(defn sql-timestamp
  "Convert the object to a java.sql.Timestamp."
  [object] (if object (java.sql.Timestamp. (to-ms object))))

(defn sql-timestamp-now
  "Returns the current time as java.sql.Timestamp."
  [] (sql-timestamp (now)))

(def path-pattern #".*(\d{4})/(\d{2})/(\d{2})/.*(\d{2})(\d{2})(\d{2}).*")

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
