(ns netcdf.utils
  (:import java.io.File)
  (:use [clojure.string :only (join)]
        [clj-time.core :only (now in-secs interval date-time year month day hour)]
        [clj-time.format :only (formatters unparse)]
        netcdf.time))

(defn date-path-fragment [time]
  (if-let [time (to-date-time time)]
    (format (join File/separator ["%4d" "%02d" "%02d"]) (year time) (month time) (day time))))

(defn time-path-fragment [time]
  (if-let [time (to-date-time time)]
    (unparse (formatters :basic-time-no-ms) time)))

(defn date-time-path-fragment [time]
  (if time
    (str (date-path-fragment time) File/separator (time-path-fragment time))))

(defn file-exists? [filename]
  (.exists (java.io.File. filename)))

(defn file-extension
  "Returns the filename extension."
  [filename] (last (re-find #"\.(.[^.]+)$" (str filename))))

(defn file-size [filename]
  (if filename (.length (java.io.File. filename))))

(defn human-duration [interval]
  (str (in-secs interval) " s"))

(defn human-file-size [filename]
  (str (file-size filename) " bytes"))

(defn human-transfer-rate [size interval]
  (str (float (/ (/ size 1000)
                 (if (> (in-secs interval) 0)
                   (in-secs interval) 1)))
       " KB/s"))

(defn nan?
  "Returns true if d is Double/NaN, otherwise false."
  [d] (Double/isNaN d))

(defn parse-integer [string & options]
  (let [{:keys [radix junk-allowed] :or {radix 10, junk-allowed false}} (apply hash-map options)]
    (try
      (Integer/parseInt (str string) radix)
      (catch NumberFormatException e
        (when-not junk-allowed
          (throw NumberFormatException e))))))

(defn with-meta+
  "Returns an object of the same type and value as obj, with map m
  merged onto the object's metadata."
  [obj m] (with-meta obj (merge (meta obj) m)))
