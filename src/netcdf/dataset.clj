(ns netcdf.dataset
  (:import ucar.nc2.dataset.NetcdfDataset
           (ucar.nc2 FileWriter NetcdfFile)
           (ucar.nc2.dt.grid GridDataset GridAsPointDataset)))

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
     (let [~symbol (FileWriter. (str file#) false)]
       ~@body
       (.finish ~symbol))))

(defn geo-grids
  "Returns all grids in the NetCDF dataset."
  [^GridDataset dataset] (.getGrids dataset))

(defn datatype-names
  "Returns all grids in the NetCDF dataset."
  [^NetcdfDataset dataset] (map #(.getName %) (geo-grids dataset)))

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
  [^GridDataset dataset] (sort (.getDates (GridAsPointDataset. (.getGrids dataset)))))

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
         (write-variables dataset writer variables)))))
