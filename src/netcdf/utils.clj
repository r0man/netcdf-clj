(ns netcdf.utils)

(defn file-extension
  "Returns the filename extension."
  [filename] (last (re-find #"\.(.[^.]+)$" (str filename))))

(defn with-meta+
  "Returns an object of the same type and value as obj, with map m
  merged onto the object's metadata."
  [obj m] (with-meta obj (merge (meta obj) m)))
