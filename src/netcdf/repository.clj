(ns netcdf.repository
  (use [clojure.contrib.def :only (defvar)]
       [netcdf.dods :only (download-variable)]))

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

(defn global-forecast-system-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map lookup-repository ["gfs-hd"])))

(defn wave-watch-repositories
  "Returns the Wave Watch III repositories."
  [] (remove nil? (map lookup-repository ["akw" "enp" "nah" "nph" "nww3" "wna"])))

(defn download-variables [repositories variables]  
  (doseq [repository repositories]
    (println (str "* " (:description repository)))
    (println (str "  Url: " (:url repository)))
    (doseq [variable variables]
      (time
       (do
         (print (str "  - " variable " "))
         (println (str (download-variable repository variable)))
         (print "    "))))))

(defn download-global-forecast-system [& variables]
  (println "Downloading the Global Forecast System Model ...")
  (download-variables (global-forecast-system-repositories) (or variables ["tmpsfc"])))

(defn download-wave-watch [& variables]
  (println "Downloading the Wave Watch III Model ...")
  (download-variables (wave-watch-repositories) (or variables ["htsgwsfc"])))

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
  "Regional Atlantic Hurricane Wave Model")

(defrepo "nph"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/nph"
  "Regional North Pacific Hurricane Wave Model")

(defrepo "nww3"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3"
  "Global NOAA Wave Watch III Model")

(defrepo "wna"
  "http://nomad5.ncep.noaa.gov:9090/dods/waves/wna"
  "Regional Western North Atlantic Wave Model")

(defrepo "gfs-hd"
  "http://nomads.ncep.noaa.gov:9090/dods/gfs_hd"
  "Global Forecast Model")

;; (download-wave-watch)
;; (download-global-forecast-system)