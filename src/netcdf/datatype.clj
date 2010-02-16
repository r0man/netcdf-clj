(ns netcdf.datatype
  (:import ucar.nc2.dt.grid.GridAsPointDataset)
  (:use netcdf.location))

(defstruct record :actual-location :distance :unit :valid-time :value :variable)

(defn- read-data [datatype valid-time location]
  (let [dataset (GridAsPointDataset. [datatype])]
    (if (and (:altitude location) (. dataset hasVert datatype (:altitude location)))
      (. dataset readData datatype valid-time (:altitude location) (:latitude location) (:longitude location))
      (. dataset readData datatype valid-time (:latitude location) (:longitude location)))))

(defn read-dataset [datatype valid-time location]
  (if location
    (let [data (read-data datatype valid-time location)]
      (struct-map record
        :actual-location (make-location (.lat data) (.lon data) (.z data))
        :distance (distance location (make-location (.lat data) (.lon data) (.z data)))
        :requested-location location
        :unit (.getUnitsString datatype)
        :valid-time valid-time
        :value (.dataValue data)
        :variable (.getName datatype)))))
