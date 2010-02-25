(ns netcdf.test.helper
  (:require [netcdf.datatype :as datatype]))

(def *dataset-uri* "/home/roman/.weather/20100215/akw.06.nc")
(def *dataset-uri* "/home/roman/.weather/20100215/nww3.06.nc")
(def *variable* "htsgwsfc")

(def *datatype* (datatype/open-datatype (datatype/make-datatype *dataset-uri* *variable*)))
(def *valid-time* (first (datatype/valid-times *datatype*)))

