(ns netcdf.dataset
  (:import (ucar.nc2 FileWriter NetcdfFile)
           (ucar.nc2.dt.grid GridAsPointDataset GridDataset)
           ucar.nc2.dataset.NetcdfDataset)
  (:require [netcdf.geo-grid :as geogrid])
  (:use [clojure.contrib.duck-streams :only (with-out-writer)]
        netcdf.time))

(defn- write-dimensions [^NetcdfDataset dataset ^FileWriter writer]
  (doseq [dimension (.getDimensions dataset)]
    (. writer writeDimension dimension)
    (. writer writeVariable (.findVariable dataset (.getName dimension)))))

(defn- write-global-attributes [^NetcdfDataset dataset ^FileWriter writer]
  (doseq [attribute (.getGlobalAttributes dataset)]
    (. writer writeGlobalAttribute attribute)))

(defn- write-variables [^NetcdfDataset dataset ^FileWriter writer variables]
  (doseq [variable variables]
    (. writer writeVariable (.findVariable dataset (str variable)))))

(defmacro with-file-writer [symbol filename & body]
  `(let [file# (java.io.File. ~filename)]
     (.mkdirs (.getParentFile file#))
     (let [~symbol (FileWriter. (.getPath (.toURI file#)) false)]
       ~@body
       (.finish ~symbol))))

(defn geo-grids
  "Returns all grids in the NetCDF dataset."
  [^GridDataset dataset] (.getGrids dataset))

(defn datatype-names
  "Returns all grids in the NetCDF dataset."
  [^NetcdfDataset dataset] (map #(.getName %) (geo-grids dataset)))

(defn find-grid-datatype
  "Find the GridDatatype by name"
  [^GridDataset dataset name] (.findGridDatatype dataset name))

(defn find-geo-grid
  "Find the GeoGrid by name"
  [^GridDataset dataset name]
  (first (filter #(= (.getName %) name) (geo-grids dataset))))

(defn open-dataset
  "Open the NetCDF dataset."
  [uri] (. NetcdfDataset openDataset (str uri)))

(defn open-grid-dataset
  "Open the NetCDF dataset as a grid dataset."
  [uri] (. GridDataset open (str uri)))

(defn valid-times
  "Returns the valid times in the NetCDF dataset."
  [^GridDataset dataset]
  (map to-date-time (sort (.getDates (GridAsPointDataset. (.getGrids dataset))))))

(defn copy-dataset
  "Copy the NetCDF dataset from source to target."
  ([source target]
     (with-open [dataset (open-grid-dataset source)]
       (copy-dataset source target (datatype-names dataset))))
  ([source target variables]
     (with-open [dataset (open-dataset source)]
       (with-file-writer writer target
         (write-global-attributes dataset writer)
         (write-dimensions dataset writer)
         (write-variables dataset writer variables))
       target)))

(defmacro with-open-dataset [[name uri] & body]
  `(with-open [~name (open-dataset ~uri)]
     ~@body))

(defmacro with-open-grid-dataset [[name uri] & body]
  `(with-open [~name (open-grid-dataset ~uri)]
     ~@body))

(defn dump-csv
  "Dump the dataset as CSV to stdout."
  [^GridDataset dataset & {:keys [separator valid-time z-coord]}]
  (doseq [grid (geo-grids dataset)]
    (geogrid/dump grid :separator separator :valid-time valid-time :z-coord z-coord)))

(defn write-csv
  "Write the dataset as CSV to filename."
  [^GridDataset dataset filename & {:keys [separator valid-time z-coord separator]}]
  (with-out-writer filename
    (dump-csv dataset :separator separator :valid-time valid-time :z-coord z-coord)))