(ns netcdf.repository
  (use [clojure.contrib.def :only (defvar)]))

(defvar *repositories* (ref {})
  "The map of repositories.")

(defrecord Repository [name url description])

(defn lookup-repository
  "Lookup a registered repository by name."
  [name] (get @*repositories* (keyword name)))

(defn make-repository [name url & [description]]
  (Repository. name url description))

(defn register-repository
  "Register the repository."
  [repository]
  (dosync (ref-set *repositories* (assoc @*repositories* (keyword (:name repository)) repository)))
  repository)

(defn repository?
  "Returns true if arg is a repository, otherwise false."
  [arg] (isa? (class arg) Repository))

(defn unregister-repository
  "Unregister the repository."
  [repository]
  (dosync (ref-set *repositories* (dissoc @*repositories* (keyword (:name repository)))))
  repository)

(defmacro defrepo
  "Define and register the repository."
  [name url & [description]]
  (register-repository (make-repository name url description)))

(defrepo "akw"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw"
  "Regional Alaska Waters Wave Model")

(defrepo "enp"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/enp"
  "Regional Eastern North Pacific Wave Model")

(defrepo "nah"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/nah"
  "Regional Regional Atlantic Hurricane Wave Model")

(defrepo "nph"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/nph"
  "Regional North Pacific Hurricane Wave Model")

(defrepo "nww3"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/nph"
  "Global NOAA Wave Watch III Wave Model")

(defrepo "wna"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/wna"
  "Regional Western North Atlantic Wave Model")

(defrepo "gfs-hd"
  "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  "Global Forecast Model")

(lookup-repository "nww3")
