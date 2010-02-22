(ns netcdf.datatype
  (:import ucar.nc2.dt.grid.GridDataset ucar.nc2.dt.grid.GridAsPointDataset ucar.unidata.geoloc.LatLonPointImpl)
  (:use incanter.core netcdf.location))

(defstruct datatype :dataset-uri :variable :service)
(defstruct record :actual-location :distance :unit :valid-time :value :variable)

(defn bounding-box
  "Returns the bounding box of the datatype."
  [datatype]
  (.. (:service datatype) getCoordinateSystem getLatLonBoundingBox))

(defn description [datatype]
  (.. (:service datatype) getVariable getDescription))

(defn latitude-axis [datatype]
  (let [axis (.. (:service datatype) getCoordinateSystem getYHorizAxis)
        bounds (bounding-box datatype)]
    {:min (.getLatMin bounds)
     :max (.getLatMax bounds)
     :size (.getSize axis)
     :step (/ (.getHeight bounds) (- (.getSize axis) 1))}))

(defn longitude-axis [datatype]
  (let [axis (.. (:service datatype) getCoordinateSystem getXHorizAxis)
        bounds (bounding-box datatype)]
    {
     :min (.getLonMin bounds)
     :max (.getLonMax bounds)
     :size (.getSize axis)
     :step (/ (.getWidth bounds) (- (.getSize axis) 1))}))

(defn latitude-range
  "Returns the range of the latitude axis."
  [datatype]
  (let [axis (latitude-axis datatype)]
    (range (:min axis) (:max axis) (:step axis))))

(defn longitude-range
  "Returns the range of the longitude axis."
  [datatype]
  (let [axis (longitude-axis datatype)]
    (range (:min axis) (:max axis) (:step axis))))

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

(defn time-index
  "Returns the time index for valid-time."
  [datatype valid-time]
  (. (. (.getCoordinateSystem (:service datatype)) getTimeAxis1D) findTimeIndexFromDate valid-time))

(defn- read-data [datatype valid-time location]
  (let [datatype (:service datatype) dataset (GridAsPointDataset. [datatype])]
    (if (and (:altitude location) (. dataset hasVert datatype (:altitude location)))
      (. dataset readData datatype valid-time (:altitude location) (:latitude location) (:longitude location))
      (. dataset readData datatype valid-time (:latitude location) (:longitude location)))))

(defn read-at-location
  "Read the NetCDF datatype for the given time and location."
  [datatype valid-time location]
  (if location
    (let [data (read-data datatype valid-time location)]
      (struct-map record
        :actual-location (make-location (.lat data) (.lon data) (.z data))
        :requested-location location
        :unit (.getUnitsString (:service datatype))
        :valid-time valid-time
        :value (.dataValue data)
        :variable (:variable datatype)))))

(defn read-seq
  "Read the whole datatype at valid-time as sequence."
  [datatype valid-time & options]
  (let [options (apply hash-map options)]
    (with-meta
      (seq (.copyTo1DJavaArray (.readYXData (:service datatype) (time-index datatype valid-time) (or (:z options) 0))))
      {:description (description datatype)
       :latitude-axis (latitude-axis datatype)
       :longitude-axis (longitude-axis datatype)
       :valid-time valid-time
       :variable (:variable datatype)})))

(defn read-matrix
  "Read the whole datatype at valid-time as matrix."
  [datatype valid-time & options]
  (let [options (apply hash-map options)]
    (with-meta
      (trans (matrix
        (apply read-seq datatype valid-time options)
        (int (:size (longitude-axis datatype)))))    
      {:description (description datatype)
       :latitude-axis (latitude-axis datatype)
       :longitude-axis (longitude-axis datatype)
       :valid-time valid-time
       :variable (:variable datatype)})))

(defn valid-times
  "Returns the valid times in the NetCDF datatype."
  [datatype]
  (if (datatype-open? datatype)
    (.. (.getCoordinateSystem (:service datatype)) getTimeAxis1D getTimeDates)
    (valid-times (open-datatype datatype))))
