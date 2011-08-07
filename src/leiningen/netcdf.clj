(ns leiningen.netcdf
  (:use netcdf.model
        netcdf.variable))

(defn download [model & variables]
  (let [model (or (get @*models* (keyword model))
                  (throw (Exception. (str "Can't find model: " model))))
        variable-names (set (or variables (map :name (:variables model))))]
    (doseq [variable (filter #(contains? variable-names (:name %)) (:variables model))]
      (download-variable model variable))))

(defn netcdf
  "NetCDF tasks."
  [project & [command & args]]
  (condp = command
    "download"
    (let [[model variables] args]
      (doseq [model (if model [model] (sort (map :name (vals @*models*))))]
        (apply download model variables)))))
