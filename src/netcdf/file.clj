(ns netcdf.file
  (:import java.io.File))

(defn netcdf-file?
  "Returns true if file is a NetCDF file, otherwise false."
  [file] (and (.exists (File. (str file))) (.endsWith (str file) ".nc")))

(defn netcdf-file-seq
  "Returns a seq of all NetCDF files in the given directory."
  [directory] (filter netcdf-file? (file-seq (File. directory))))
