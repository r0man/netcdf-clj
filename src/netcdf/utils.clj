(ns netcdf.utils)

(defn file-exists? [filename]
  (.exists (java.io.File. filename)))

(defn file-extension
  "Returns the filename extension."
  [filename] (last (re-find #"\.(.[^.]+)$" (str filename))))

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
