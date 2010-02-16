(ns netcdf.datatype
  (:import ucar.nc2.dt.grid.GridAsPointDataset)
  (:use netcdf.location))

(defstruct record :actual-location :distance :unit :valid-time :value :variable)

(defn- read-data [datatype valid-time location]
  (let [dataset (GridAsPointDataset. [datatype])]
    (if (and (:altitude location) (. dataset hasVert datatype (:altitude location)))
      (. dataset readData datatype valid-time (:altitude location) (:latitude location) (:longitude location))
      (. dataset readData datatype valid-time (:latitude location) (:longitude location)))))

(defn read-datatype [datatype valid-time location]
  (if location
    (let [data (read-data datatype valid-time location)
          actual-location (make-location (.lat data) (.lon data) (.z data))]
      (struct-map record
        :actual-location actual-location
        :distance (distance location actual-location)
        :requested-location location
        :unit (.getUnitsString datatype)
        :valid-time valid-time
        :value (.dataValue data)
        :variable (.getName datatype)))))

(defn valid-times [datatype]
  (.. (.getCoordinateSystem datatype) getTimeAxis1D getTimeDates))
