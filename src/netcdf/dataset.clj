(ns netcdf.dataset
  (:import ucar.nc2.FileWriter
           ucar.nc2.NetcdfFile
           ucar.nc2.dataset.NetcdfDataset
           [ucar.nc2.dt.grid GridDataset GridAsPointDataset]))

(defn- write-dimensions [dataset writer]
  (let [dimensions (.getDimensions dataset)]
    (dorun (map #(. writer writeDimension %) dimensions))
    (dorun (map #(. writer writeVariable (.findVariable dataset (.getName %))) dimensions))))

(defn- write-global-attributes [dataset writer]
  (dorun (map #(. writer writeGlobalAttribute %) (.getGlobalAttributes dataset))))

(defn- write-variables [dataset writer variables]
  (dorun (map #(. writer writeVariable (.findVariable dataset %1)) variables)))

(defmacro with-file-writer [symbol filename & body]
  `(let [~symbol (ucar.nc2.FileWriter. ~filename false)]
     (.mkdirs (.getParentFile (java.io.File. ~filename)))
     ~@body
     (. ~symbol finish)))

(defn open-grid-dataset
  "Open the NetCDF grid dataset at the given URI."
  [uri] (. GridDataset open uri))

(defn open-dataset
  "Open the NetCDF dataset at the given URI."
  [uri] (. NetcdfDataset open uri))

(defn close-dataset
  "Close the given dataset."
  [dataset] (if dataset (.close dataset)))

(defn copy-dataset
  "Copy the NetCDF dataset from source to target."
  [source target variables]
  (with-open [dataset (open-dataset source)]
    (with-file-writer writer target
      (write-global-attributes dataset writer)
      (write-dimensions dataset writer)
      (write-variables dataset writer variables))))

(defmulti datatypes
  "Returns all datatypes in the NetCDF dataset."
  class)

(defmethod datatypes GridDataset [dataset]
  (.getGrids dataset))

(defmulti datatype
  "Returns the datatype in the NetCDF dataset."
  (fn [dataset variables] (class dataset)))

(defmethod datatype GridDataset [dataset variable]
  (. dataset findGridDatatype variable))

(defn valid-times [dataset]
  (sort (.getDates (GridAsPointDataset. (.getGrids dataset)))))
