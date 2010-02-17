(ns netcdf.datatype
  (:import ucar.nc2.dt.grid.GridAsPointDataset ucar.unidata.geoloc.LatLonPointImpl)
  (:use netcdf.location)
  (:require [netcdf.dataset :as dataset]))

(defstruct datatype :dataset-uri :variable :service)
(defstruct record :actual-location :distance :unit :valid-time :value :variable)

(defn- read-data [datatype valid-time location]
  (let [datatype (:service datatype) dataset (GridAsPointDataset. [datatype])]
    (if (and (:altitude location) (. dataset hasVert datatype (:altitude location)))
      (. dataset readData datatype valid-time (:altitude location) (:latitude location) (:longitude location))
      (. dataset readData datatype valid-time (:latitude location) (:longitude location)))))

(defn bounding-box
  "Returns the bounding box of the datatype."
  [datatype]
  (.. (:service datatype) getCoordinateSystem getLatLonBoundingBox))

(defn latitude-range [datatype]
  (let [bounds (bounding-box datatype)
        step (.. (:service datatype) getCoordinateSystem getYHorizAxis getSize)]
    (range (.getLatMin bounds) (.getLatMax bounds) (/ (.getHeight bounds) (- step 1)))))

(defn longitude-range [datatype]
  (let [bounds (bounding-box datatype)
        step (.. (:service datatype) getCoordinateSystem getXHorizAxis getSize)]
    (range (- (.getLonMin bounds) 180) (- (.getLonMax bounds) 180) (/ (.getWidth bounds) (- step 1)))))

(defn make-datatype
  "Make a NetCDF datatype."
  [dataset-uri variable]
  (struct datatype dataset-uri variable))

(defn datatype-open?
  "Returns true if the datatype is open, else false."
  [datatype]
  (not (nil? (:service datatype))))

(defn open-datatype
  "Open the NetCDF datatype."
  [datatype]
  (if-not (datatype-open? datatype)
    (let [dataset (dataset/open-grid-dataset (dataset/make-dataset (:dataset-uri datatype)))]
      (assoc datatype :service (. (:service dataset) findGridDatatype (:variable datatype))))))

(defn read-at-location
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [data (read-data datatype valid-time location)
          actual-location (make-location (.lat data) (.lon data) (.z data))]
      (struct-map record
        :actual-location actual-location
        :distance (distance location actual-location)
        :requested-location location
        :unit (.getUnitsString (:service datatype))
        :valid-time valid-time
        :value (.dataValue data)
        :variable (:variable datatype)))))

(defn read-datatype
  "Read the whole NetCDF datatype for the given time."
  [datatype valid-time & options]
  (let [options (apply hash-map options)
        lat-range (latitude-range datatype)
        lon-range (longitude-range datatype)]
    (for [latitude lat-range longitude lon-range]
      (read-at-location datatype valid-time (make-location latitude longitude)))))

(defn valid-times
  "Returns the valid times in the NetCDF datatype."
  [datatype]
  (if (datatype-open? datatype)
    (.. (.getCoordinateSystem (:service datatype)) getTimeAxis1D getTimeDates)
    (valid-times (open-datatype datatype))))
