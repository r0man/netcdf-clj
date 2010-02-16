(ns netcdf.dataset
  (:import ucar.nc2.FileWriter
           ucar.nc2.NetcdfFile
           ucar.nc2.dataset.NetcdfDataset
           [ucar.nc2.dt.grid GridDataset GridAsPointDataset]))

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

(defn make-dataset [uri]
  (struct dataset uri nil))

(defn open-grid-dataset
  "Open the NetCDF grid dataset."
  [dataset]
  (assoc dataset :service(. GridDataset open (:uri dataset))))

(defn open-dataset
  "Open the NetCDF dataset."
  [dataset]
  (assoc dataset :service(. NetcdfDataset open (:uri dataset))))

(defn close-dataset
  "Close the given dataset."
  [dataset]
  (if-let [service (:service dataset)]
    (.close service)))

(defn copy-dataset
  "Copy the NetCDF dataset from source to target."
  [source target variables]
  (with-open [dataset (:service (open-dataset (make-dataset source)))]
    (with-file-writer writer target
      (write-global-attributes dataset writer)
      (write-dimensions dataset writer)
      (write-variables dataset writer variables))))

(defn datatype [dataset variable]
  (. (:service dataset) findGridDatatype variable))

(defn datatypes [dataset]
  (.getGrids (:service dataset)))

(defn valid-times
  "Returns the valid times in the NetCDF dataset."
  [dataset] (sort (.getDates (GridAsPointDataset. (.getGrids (:service dataset))))))
