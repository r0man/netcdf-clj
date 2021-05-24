(ns netcdf.datafy
  (:require [clojure.core.protocols :refer [Datafiable]]
            [clojure.datafy :refer [datafy]]))

(extend-type ucar.ma2.DataType
  Datafiable
  (datafy [data-type]
    (keyword (str data-type))))

(extend-type ucar.nc2.Attribute
  Datafiable
  (datafy [attribute]
    {:name (.getName attribute)
     :values (mapv #(.getValue attribute %) (range (.getLength attribute)))}))

(extend-type ucar.nc2.dataset.CoordinateSystem
  Datafiable
  (datafy [coordinate-system]
    {:name (.getName coordinate-system)
     :projection (datafy (.getProjection coordinate-system))}))

(extend-type ucar.nc2.dataset.NetcdfDataset
  Datafiable
  (datafy [dataset]
    {:convention (.getConventionUsed dataset)
     :global-attributes (mapv datafy (.getGlobalAttributes dataset))
     :variables (mapv datafy (.getVariables dataset))}))

(extend-type ucar.nc2.dataset.VariableDS
  Datafiable
  (datafy [variable]
    {:coordinate-systems (mapv datafy (.getCoordinateSystems variable))
     :data-type (datafy (.getDataType variable))
     :dataset-location (.getDatasetLocation variable)
     :description (.getDescription variable)
     :dimensions (mapv datafy (.getDimensions variable))
     :fill-value (.getFillValue variable)
     :missing-values (.getMissingValues variable)
     :name (.getName variable)
     :size (.getSize variable)
     :units (.getUnitsString variable)}))

(extend-type ucar.nc2.Dimension
  Datafiable
  (datafy [dimension]
    {:length (.getLength dimension)
     :name (.getName dimension)}))

(extend-type ucar.unidata.geoloc.Projection
  Datafiable
  (datafy [projection]
    {:name (.getName projection)
     :params (mapv datafy (.getProjectionParameters projection))}))

(extend-type ucar.unidata.util.Parameter
  Datafiable
  (datafy [parameter]
    (cond-> {:name (.getName parameter)}
      (.isString parameter)
      (assoc :values [(.getStringValue parameter)])
      (not (.isString parameter))
      (assoc :values (.getNumericValues parameter)))))
