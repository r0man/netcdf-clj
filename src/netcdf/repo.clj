(ns netcdf.repo)

(defprotocol IRepository
  (-reference-times [repository model]))

(defrecord LocalRepository [root])

(extend-type LocalRepository
  IRepository
  (-reference-times [repository model]))

(defn make-local-repository
  "Make a local repository."
  [url] (LocalRepository. url))
