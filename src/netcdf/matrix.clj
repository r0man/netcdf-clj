(ns netcdf.matrix
  (:import incanter.Matrix)
  (:use incanter.core
        [clojure.contrib.string :only (replace-re)]
        [netcdf.time :only (format-time parse-time)]))

(defn- meta-data-filename
  "Returns the filename that contains the matrix meta data."
  [filename]
  (if (re-matches #".*(\.[^.]+)$" filename)
    (replace-re #"(.*)(\.[^.]+)$" "$1.meta" filename)
    (str filename ".meta")))

(defn- serialize-meta
  "Serialize the matrix meta data."
  [meta] (assoc meta :valid-time (format-time (:valid-time meta))))

(defn- deserialize-meta
  "Deserialize the matrix meta data."
  [meta] (assoc meta :valid-time (parse-time (:valid-time meta))))

(defn- deserialize-seq
  "Deserialize the matrix seq by replacing all NaN symbols with
  Double/NaN."
  [sequence] (map #(if (= % 'NaN) Double/NaN %) sequence) )

(defn write-meta-data
  "Write the matrix meta data to filename."
  [#^Matrix matrix filename]    
  (spit filename (prn-str (serialize-meta (meta matrix))))
  filename)

(defn read-meta-data
  "Read the matrix meta data from filename."
  [filename] (deserialize-meta (read-string (slurp filename))))

(defn write-matrix
  "Write the matrix to filename."
  [#^Matrix matrix filename]
  (write-meta-data matrix (meta-data-filename filename))
  (spit (meta-data-filename filename) (prn-str (serialize-meta (meta matrix))))
  (spit filename (prn-str matrix))
  filename)

(defn read-matrix
  "Read the matrix from filename."
  [filename]
  (let [meta (read-meta-data (meta-data-filename filename))
        columns (:size (:lon-axis meta))]
    (with-meta (matrix (deserialize-seq (read-string (slurp filename))) columns)
      meta)))
