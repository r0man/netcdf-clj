(ns netcdf.dataset
  (:import ucar.nc2.FileWriter
           ucar.nc2.NetcdfFile
           ucar.nc2.dataset.NetcdfDataset
           [ucar.nc2.dt.grid GridDataset GridAsPointDataset])
  (:require [netcdf.datatype :as datatype]))

(defstruct dataset :uri :service)

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

(defn make-dataset
  "Make a NetCDF dataset."
  [uri]
  (struct dataset uri nil))

(defn dataset-open?
  "Returns true if the NetCDF dataset is open, else false."
  [dataset]
  (not (nil? (:service dataset))))

(defn open-grid-dataset
  "Open the NetCDF dataset as a grid dataset."
  [dataset]
  (assoc dataset :service (. GridDataset open (:uri dataset))))

(defn open-dataset
  "Open the NetCDF dataset."
  [dataset]
  (if-not (dataset-open? dataset)
    (assoc dataset :service(. NetcdfDataset open (:uri dataset)))))

(defn close-dataset
  "Close the NetCDF dataset."
  [dataset]
  (if-let [service (:service dataset)]
    (.close service)))

(defn datatype
  "Returns the datatype in the NetCDF dataset for the variable."
  [dataset variable]
  (if-let [service (. (:service dataset) findGridDatatype variable)]
    (assoc (datatype/make-datatype (:uri dataset) variable) :service service)))

(defn datatypes
  "Returns all datatypes in the NetCDF dataset."
  [dataset]
  (map #(assoc (datatype/make-datatype (:uri dataset) (.getName %)) :service %)
       (.getGrids (:service dataset))))

(defn read-dataset [dataset valid-time]
  (let [datatypes (datatypes dataset)]
    (hash-map
     (map (comp keyword :variable) datatypes)
     (map #(datatype/read-datatype % valid-time) datatypes))))

(defn valid-times
  "Returns the valid times in the NetCDF dataset."
  [dataset] (sort (.getDates (GridAsPointDataset. (.getGrids (:service dataset))))))

(defn copy-dataset
  "Copy the NetCDF dataset from source to target."
  ([source target]
     (copy-dataset source target (map :variable (datatypes (open-grid-dataset (make-dataset source))))))
  ([source target variables]
     (with-open [dataset (:service (open-dataset (make-dataset source)))]
       (with-file-writer writer target
         (write-global-attributes dataset writer)
         (write-dimensions dataset writer)
         (write-variables dataset writer variables)))))
