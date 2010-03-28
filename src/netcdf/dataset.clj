(ns netcdf.dataset
  (:import ucar.nc2.dataset.NetcdfDataset
           (ucar.nc2 FileWriter NetcdfFile)
           (ucar.nc2.dt.grid GridDataset GridAsPointDataset)))

(defn- write-dimensions [#^NetcdfDataset dataset #^FileWriter writer]
  (let [dimensions (.getDimensions dataset)]
    (dorun (map #(. writer writeDimension %) dimensions))
    (dorun (map #(. writer writeVariable (.findVariable dataset (.getName %))) dimensions))))

(defn- write-global-attributes [#^NetcdfDataset dataset #^FileWriter writer]
  (dorun (map #(. writer writeGlobalAttribute %) (.getGlobalAttributes dataset))))

(defn- write-variables [#^NetcdfDataset dataset #^FileWriter writer variables]
  (dorun (map #(. writer writeVariable (.findVariable dataset (str %1))) variables)))

(defmacro with-file-writer [symbol filename & body]
  (let [filename# filename symbol# symbol]
    `(let [~symbol# (FileWriter. (str ~filename#) false)]
       (.mkdirs (.getParentFile (java.io.File. ~filename#)))
       ~@body
       (. ~symbol# finish))))

(defn grids
  "Returns all grids in the NetCDF dataset."
  [#^NetcdfDataset dataset] (.getGrids dataset))

(defn open-grid-dataset
  "Open the NetCDF dataset as a grid dataset."
  [uri] (. GridDataset open (str uri)))

(defn open-dataset
  "Open the NetCDF dataset."
  [uri] (. NetcdfDataset openDataset (str uri)))

(defn valid-times
  "Returns the valid times in the NetCDF dataset."
  [#^NetcdfDataset dataset] (sort (.getDates (GridAsPointDataset. (.getGrids dataset)))))

(defn copy-dataset
  "Copy the NetCDF dataset from source to target."
  ([source target]
     (with-open [dataset (open-grid-dataset source)]
       (copy-dataset source target (map #(.getName %) (grids dataset)))))
  ([source target variables]
     (with-open [dataset (open-dataset source)]
       (with-file-writer writer target
         (write-global-attributes dataset writer)
         (write-dimensions dataset writer)
         (write-variables dataset writer variables)))))
