(ns netcdf.forecast
  (:use [netcdf.geo-grid :only (interpolate-location open-geo-grid valid-times)]
        netcdf.model
        netcdf.location
        netcdf.repository
        [clojure.data.json :ony (read-json)]))

(def ^:dynamic *cache* (atom {}))

(defn open-grid
  "Lookup the geo grid in *cache*, or open it."
  [model variable reference-time]
  (let [path (variable-path model variable reference-time)]
    (or (get @*cache* path)
        (let [grid (open-geo-grid path (:name variable))]
          (swap! *cache* assoc path grid)
          grid))))

(defn read-variables
  [variables location reference-time]
  (flatten
   (for [variable variables
         :let [model (best-model-for-location (:models variable) location)]
         :when model]
     (let [grid (open-grid model variable reference-time)]
       (for [valid-time (valid-times grid)]
         {:model (:name model)
          :unit (:unit variable)
          :value (interpolate-location grid location :valid-time valid-time)
          :valid-time valid-time
          :variable (:name variable)})))))

;; (best-model-for-location )

;; (:models (lookup-variable "ugrdsfc"))

(defn read-forecast
  "Read the forecast at location for the given variables and the
  reference-time."
  [variables location reference-time]
  (map
   (fn [measures]
     (reduce #(assoc %1 (:variable %2) (dissoc %2 :variable)) {} measures))
   (vals (group-by :valid-time (read-variables variables location reference-time)))))

;; (read-forecast (vals @*variables*) (make-location 0 0) "2011-08-14T00:00:00Z")

;; (doseq [spot (take 2 (map read-json (read-lines "/home/roman/workspace/magicseaweed/magicseaweed.json")))
;;         forecast (read-forecast (vals @*variables*) (make-location (:latitude (:location spot)) (:longitude (:location spot))) "2011-08-14T00:00:00Z")
;;         :when (and (:latitude (:location spot)) (:longitude (:location spot)))]
;;   forecast)

;; (time
;;  (with-out-writer "/tmp/forecast.csv"
;;    (doseq [spot (map read-json (read-lines "/home/roman/workspace/magicseaweed/magicseaweed.json"))
;;            forecast (read-forecast (vals @*variables*) (make-location (:latitude (:location spot)) (:longitude (:location spot))) "2011-08-14T00:00:00Z")
;;            :when (and (:latitude (:location spot)) (:longitude (:location spot)))]
;;      (println forecast))))

