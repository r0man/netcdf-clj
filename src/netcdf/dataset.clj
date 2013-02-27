(ns netcdf.dataset
  (:import [ucar.nc2 FileWriter NetcdfFile]
           [ucar.nc2.dt.grid GridAsPointDataset GridDataset]
           java.io.File
           ucar.nc2.dataset.NetcdfDataset
           ucar.nc2.geotiff.GeotiffWriter)
  (:require [clj-time.core :refer [year month day hour]]
            [clj-time.coerce :refer [to-date-time]]
            [clj-time.format :refer [formatters unparse]]
            [clojure.java.io :refer [delete-file file make-parents]]
            [clojure.tools.logging :refer [debugf]]
            [netcdf.geo-grid :as geogrid]
            [netcdf.utils :refer [save-md5-checksum valid-md5-checksum? with-out-writer]]
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
     (try
       (when-not (valid-md5-checksum? target)
         (with-open [dataset (open-dataset source)]
           (with-file-writer writer target
             (write-global-attributes dataset writer)
             (write-dimensions dataset writer)
             (write-variables dataset writer variables)))
         (save-md5-checksum target))
       target
       (catch Exception e
         (delete-file target true)
         (throw e)))))

(defmacro with-dataset [[name uri] & body]
  `(with-open [~name (open-dataset ~uri)]
     ~@body))

(defmacro with-grid-dataset [[name uri] & body]
  `(with-open [~name (open-grid-dataset ~uri)]
     ~@body))

(defn dump-dataset
  "Dump the dataset to stdout."
  [^GridDataset dataset & {:keys [printer valid-time z-coord]}]
  (doseq [grid (geo-grids dataset)]
    (geogrid/dump-grid grid :printer printer :valid-time valid-time :z-coord z-coord)))

(defn write-dataset
  "Write the dataset to filename."
  [^GridDataset dataset filename & {:keys [printer valid-time z-coord separator]}]
  (with-out-writer filename
    (dump-dataset dataset :printer printer :valid-time valid-time :z-coord z-coord)))

(defn geotiff-filename
  "Returns the GeoTIFF filename for `variable` at `time`."
  [variable time]
  (format "%s/%4d/%02d/%02d/%02d.tif"
          variable (year time) (month time) (day time) (hour time)))

(defn write-geotiff
  "Write the `variable` at `time` in `dataset` as a GeoTiff image to
  `filename`."
  [^GridDataset dataset variable time filename & [grey-scale]]
  (let [grid (find-geo-grid dataset variable)
        index (geogrid/time-index grid time)]
    (assert grid (format "Couldn't find geo grid %s." variable))
    (assert index (format "Couldn't find time index for %s." time))
    (make-parents filename)
    (debugf "Writing %s GeoTIFF at %s to %s." variable time filename)
    (with-open [writer (GeotiffWriter. (str filename))]
      (let [data (.readVolumeData grid index)]
        (.writeGrid writer dataset grid data (boolean grey-scale))))
    (save-md5-checksum filename)
    filename))

(defn write-geotiffs
  "Write the `variables of `dataset` to `directory`."
  [^GridDataset dataset variables directory & [grey-scale]]
  (.mkdirs (file directory))
  (doall (for [variable variables, time (valid-times dataset)
               :let [file (file directory (geotiff-filename variable time))]]
           (write-geotiff dataset variable time file grey-scale))))
