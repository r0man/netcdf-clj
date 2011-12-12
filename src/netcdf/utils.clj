(ns netcdf.utils
  (:import java.io.File)
  (:use [clojure.string :only (join)]
        [clojure.java.io :only (reader writer)]
        [clj-time.core :only (now in-secs interval date-time year month day hour)]
        [clj-time.format :only (formatters unparse)]
        [digest :only (digest)]
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

(defmacro with-out-writer
  "Opens a writer on f, binds it to *out*, and evalutes body.
Anything printed within body will be written to f."
  [f & body]
  `(with-open [stream# (writer ~f)]
     (binding [*out* stream#]
       ~@body)))

(defn md5-checksum
  "Returns the MD5 checksum of filename."
  [filename] (digest "md5" (File. filename)))

(defn save-md5-checksum
  "Save the MD5 checksum of filename."
  [filename]
  (with-out-writer (str filename ".md5")
    (println (md5-checksum filename))))

(defn valid-md5-checksum?
  "Returns the MD5 checksum of filename."
  [filename]
  (let [md5-filename (str filename ".md5")]
    (and (.exists (File. md5-filename))
         (= (md5-checksum filename)
            (first (line-seq (reader md5-filename)))))))
