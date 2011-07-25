(ns netcdf.repository)

(defprotocol Repository)

(defrecord LocalRepository [uri])

(defn local-repository
  "Make a local repository."
  [directory] (LocalRepository. directory))
