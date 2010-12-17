(ns netcdf.tasks
  (:use cake.core
        [netcdf.model :only (download-global-forecast-system download-wave-watch)]))

(deftask global-forecast-system
  "Download the Global Forecast System weather model."
  (download-global-forecast-system))

(deftask wave-watch
  "Download the Wave Watch III weather model."
  (download-wave-watch))

