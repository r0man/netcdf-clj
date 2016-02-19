(ns netcdf.utils
  (:require [clj-time.coerce :refer [to-date-time]]
            [clj-time.core :refer [now in-seconds interval date-time year month day hour]]
            [clj-time.format :refer [formatters unparse]]
            [clojure.java.io :refer [file reader writer]]
            [digest :refer [digest]]))

(defn file-exists? [filename]
  (.exists (file filename)))

(defn file-extension
  "Returns the filename extension."
  [filename] (last (re-find #"\.(.[^.]+)$" (str filename))))

(defn file-size [filename]
  (if filename (.length (file filename))))

(defn netcdf-file?
  "Returns true if file is a NetCDF file, otherwise false."
  [filename]
  (and (file-exists? filename)
       (.endsWith (str filename) ".nc")))

(defn netcdf-file-seq
  "Returns a seq of all NetCDF files in the given directory."
  [directory] (filter netcdf-file? (file-seq (file (str directory)))))

(defn human-duration [interval]
  (str (in-seconds interval) " s"))

(defn human-file-size [filename]
  (str (file-size filename) " bytes"))

(defn human-transfer-rate [size interval]
  (str (float (/ (/ size 1000)
                 (if (> (in-seconds interval) 0)
                   (in-seconds interval) 1)))
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
          (throw e))))))

(defmacro with-out-writer
  "Opens a writer on f, binds it to *out*, and evalutes body.
  Anything printed within body will be written to f."
  [f & body]
  `(with-open [stream# (writer ~f)]
     (binding [*out* stream#]
       ~@body)))

(defn md5-checksum
  "Returns the MD5 checksum of filename."
  [filename] (digest "md5" (file filename)))

(defn save-md5-checksum
  "Save the MD5 checksum of filename."
  [filename]
  (with-out-writer (str filename ".md5")
    (println (md5-checksum filename))))

(defn valid-md5-checksum?
  "Returns true if the MD5 checksum of filename is valid, otherwise
  false."
  [filename]
  (let [md5-filename (str filename ".md5")]
    (and (file-exists? filename)
         (file-exists? md5-filename)
         (= (md5-checksum filename)
            (first (line-seq (reader md5-filename)))))))

(defn with-meta+
  "Returns an object of the same type and value as obj, with map m
  merged onto the object's metadata."
  [obj m] (with-meta obj (merge (meta obj) m)))
