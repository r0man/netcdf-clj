(ns netcdf.repository)

(defprotocol Repository)

(defrecord LocalRepository [uri])

(defn local-repository
  "Make a local repository."
  [directory] (LocalRepository. directory))

(defn netcdf-file-seq
  "Returns a seq of all NetCDF files in dirctory."
  [directory]
  (filter #(.endsWith (str %) ".nc") (file-seq (java.io.File. directory))))
