(ns netcdf.datatype
  (:import ucar.nc2.dt.grid.GridDataset ucar.nc2.dt.grid.GridAsPointDataset ucar.unidata.geoloc.LatLonPointImpl)
  (:use netcdf.location))

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

(defn latitude-axis [datatype]
  (let [bounds (bounding-box datatype) axis-size (.. (:service datatype) getCoordinateSystem getYHorizAxis getSize)]
    {:min (.getLatMin bounds)
     :max (.getLatMax bounds)
     :step-size (/ (.getHeight bounds) (- axis-size 1))}))

(defn longitude-axis [datatype]
  (let [bounds (bounding-box datatype) axis-size (.. (:service datatype) getCoordinateSystem getXHorizAxis getSize)]
    {
     ;; :min (- (.getLonMin bounds) 180)
     ;; :max (- (.getLonMax bounds) 180)
     :min (.getLonMin bounds)
     :max (.getLonMax bounds)
     :step-size (/ (.getWidth bounds) (- axis-size 1))}))

(defn latitude-range
  "Returns the range of the latitude axis."
  [datatype]
  (let [axis (latitude-axis datatype)]
    (range (:min axis) (:max axis) (:step-size axis))))

(defn longitude-range
  "Returns the range of the longitude axis."
  [datatype]
  (let [axis (longitude-axis datatype)]
    (range (:min axis) (:max axis) (:step-size axis))))

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
    (let [grid-dataset (. GridDataset open (:dataset-uri datatype))]
      (assoc datatype :service (. grid-dataset findGridDatatype (:variable datatype))))))

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
        lat-range (or (:lat-range options) (latitude-range datatype))
        lon-range (or (:lon-range options) (longitude-range datatype))]
    (for [latitude lat-range longitude lon-range]
      (read-at-location datatype valid-time (make-location latitude longitude)))))

(defn valid-times
  "Returns the valid times in the NetCDF datatype."
  [datatype]
  (if (datatype-open? datatype)
    (.. (.getCoordinateSystem (:service datatype)) getTimeAxis1D getTimeDates)
    (valid-times (open-datatype datatype))))
