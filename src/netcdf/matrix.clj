(ns netcdf.matrix
  (:require [netcdf.geo-grid :as grid])
  (:import incanter.Matrix
           (java.awt Color Dimension))
  (:use incanter.core
        [clojure.contrib.string :only (replace-re)]
        [netcdf.time :only (format-time parse-time)]
        netcdf.image))

(defn- meta-data-filename
  "Returns the filename that contains the matrix meta data."
  [filename]
  (if (re-matches #".*(\.[^.]+)$" filename)
    (replace-re #"(.*)(\.[^.]+)$" "$1.meta" filename)
    (str filename ".meta")))

(defn- serialize-meta
  "Serialize the matrix meta data."
  [meta] (assoc meta :valid-time (format-time (:valid-time meta))))

(defn- deserialize-meta
  "Deserialize the matrix meta data."
  [meta] (assoc meta :valid-time (parse-time (:valid-time meta))))

(defn- deserialize-seq
  "Deserialize the matrix seq by replacing all NaN symbols with
  Double/NaN."
  [sequence] (map #(if (= % 'NaN) Double/NaN %) sequence) )

(defn write-meta-data
  "Write the matrix meta data to filename."
  [^Matrix matrix filename]
  (spit filename (prn-str (serialize-meta (meta matrix))))
  filename)

(defn read-meta-data
  "Read the matrix meta data from filename."
  [filename] (deserialize-meta (read-string (slurp filename))))

(defn write-matrix
  "Write the matrix to filename."
  [^Matrix matrix filename]
  (write-meta-data matrix (meta-data-filename filename))
  (spit filename (prn-str matrix))
  filename)

(defn- read-seq
  "Read the matrix sequence from filename."
  [filename] (deserialize-seq (read-string (slurp filename))))

(defn read-matrix
  "Read the matrix from filename."
  [filename]
  (let [meta (read-meta-data (meta-data-filename filename))]
    (with-meta
      (matrix (read-seq filename) (:size (:longitude-axis meta)))
      meta)))

(defn value->color [^Double value]
  (cond
   (.isNaN value) Color/BLACK
   (> value 10.5) (Color. 213 159 0)
   (> value 9.75) (Color. 213 0 0)
   (> value 9.0) (Color. 255 0 0)
   (> value 8.25) (Color. 255 73 0)
   (> value 7.5) (Color. 255 144 0)
   (> value 6.75) (Color. 255 196 0)
   (> value 6.0) (Color. 255 236 0)
   (> value 5.25) (Color. 255 255 102)
   (> value 4.5) (Color. 207 255 255)
   (> value 3.75) (Color. 175 246 255)
   (> value 3.0) (Color. 157 239 255)
   (> value 2.25) (Color. 109 193 255)
   (> value 1.5) (Color. 66 151 255)
   (> value 0.75) (Color. 32 80 255)
   :else (Color. 5 15 217)))

(defn render-image [matrix image]
  (doseq [row (range 0 (nrow matrix)) col (range 0 (ncol matrix))
          :let [value (sel matrix :rows row :cols col)
                ;; color (if (.isNaN value) Color/BLACK Color/WHITE)
                color (value->color value)
                ]]
    (.setRGB image col row (.getRGB color)))
  image)

(defn make-image [matrix]
  (let [image (make-buffered-image (ncol matrix) (nrow matrix))]
    (render-image matrix image)))

(defn save-as-image [matrix filename]
  (write-buffered-image (make-image matrix) filename))

;; (def *akw* (grid/read-matrix (grid/open-geo-grid "/home/roman/.netcdf/akw/htsgwsfc/20100828/t12z.nc" "htsgwsfc")))
;; (def *nww3* (grid/read-matrix (grid/open-geo-grid "/home/roman/.netcdf/nww3/htsgwsfc/20100828/t12z.nc" "htsgwsfc")))
;; (def *wna* (grid/read-matrix (grid/open-geo-grid "/home/roman/.netcdf/wna/htsgwsfc/20100828/t12z.nc" "htsgwsfc")))
;; (def *example* (grid/read-matrix (grid/open-geo-grid "/tmp/netcdf-test.nc" "htsgwsfc")))

;; (defn find-bounds [& matrixes]
;;   )

;; (defn location->position [^Matrix matrix location]
;;   (let [{:keys [latitude-axis lon-ax
;;                 is]} (meta matrix)
;;         lat-diff (- (:max latitude-axis) (:min latitude-axis))
;;         lon-diff (- (:max longitude-axis) (:min longitude-axis))
;;         ]
;;     [lat-diff lon-diff]))

;; (location->position *nww3* {:latitude 0 :longitude 0})

;; (meta *akw*)
;; (meta *wna*)
;; (meta *nww3*)


;; (time
;;  (let [model "nww3"
;;        matrix *nww3*
;;        meta (meta matrix)]
;;    (println meta)
;;    (write-buffered-image (render-matrix matrix) (str "/tmp/" model ".png"))))

;; (nrow *matrix*)
;; (ncol *matrix*)

;; (def *label* (javax.swing.JLabel. (javax.swing.ImageIcon. *image*)))

;; (.add *panel* *label*)
;; (.pack *frame*)
;; (.removeAll *frame*)
;; (.removeAll *panel*)
