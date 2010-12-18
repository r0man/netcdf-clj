(ns netcdf.utils
  (:use [clj-time.core :only (now in-secs interval date-time year month day hour)]))

(defn file-exists? [filename]
  (.exists (java.io.File. filename)))

(defn file-extension
  "Returns the filename extension."
  [filename] (last (re-find #"\.(.[^.]+)$" (str filename))))

(defn file-size [filename]
  (.length (java.io.File. filename)))

(defn human-duration [interval]
  (str (in-secs interval) " s"))

(defn human-file-size [filename]
  (str (file-size filename) " bytes"))

(defn human-transfer-rate [size interval]
  (str (float (/ (/ size 1000)
                 (if (> (in-secs interval) 0)
                   (in-secs interval) 1)))
       " KB/s"))

(defn parse-integer [string & options]
  (let [{:keys [radix junk-allowed] :or {radix 10, junk-allowed false}} (apply hash-map options)]
    (try
     (Integer/parseInt string radix)
     (catch NumberFormatException e
       (when-not junk-allowed
         (throw NumberFormatException e))))))

(defn with-meta+
  "Returns an object of the same type and value as obj, with map m
  merged onto the object's metadata."
  [obj m] (with-meta obj (merge (meta obj) m)))
