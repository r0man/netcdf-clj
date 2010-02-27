(ns netcdf.render
  (:import java.awt.image.BufferedImage
           (java.awt Color Dimension)
           (java.awt.event KeyListener)
           (javax.swing JFrame JOptionPane JPanel))
  (:use netcdf.datatype
        netcdf.interpolation
        netcdf.location
        google.maps.static
        google.maps.projection
        incanter.core
        incanter.chrono))

(defn create-panel [width height]
  (proxy [JPanel KeyListener] [] 
    (getPreferredSize [] (Dimension. width height))
    (keyPressed [e])
    (keyReleased [e]) 
    (keyTyped [e])))

(defn configure-display [frame panel]
  (doto panel
    (.setBackground Color/black)
    (.setFocusable true)
    (.addKeyListener panel))
  (doto frame
    (.add panel)
    (.pack)
    (.setVisible true))
  panel)

(defn create-display
  ([] (create-display 360 180))
  ([width height] (configure-display (JFrame.) (create-panel width height))))

(defn file-extension [filename]
  (last (re-find #"\.(.[^.]+)$" filename)))

(defn value->color [value]
  (cond
   (> value 7.0) (Color. 213 159 0)
   (> value 6.5) (Color. 213 0 0)
   (> value 6.0) (Color. 255 0 0)
   (> value 5.5) (Color. 255 73 0)
   (> value 5.0) (Color. 255 144 0)
   (> value 4.5) (Color. 255 196 0)
   (> value 4.0) (Color. 255 236 0)
   (> value 3.5) (Color. 255 255 102)
   (> value 3.0) (Color. 207 255 255)
   (> value 2.5) (Color. 175 246 255)
   (> value 2.0) (Color. 157 239 255)
   (> value 1.5) (Color. 109 193 255)
   (> value 1.0) (Color. 66 151 255)
   (> value 0.5) (Color. 32 80 255)
   (> value 0.0) (Color. 5 15 217)
;   (> value 0.0) (Color. 152 178 203)   
   ;; :else (Color. 153 179 204)
   :else Color/black
   ))

(defn value->color [value]
  (cond
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
   ;; (> value 0.5) (Color. 5 15 217)
   ;; (> value 0.) (Color. 152 178 203)
   :else (Color. 5 15 217)
   ;; :else (Color. 153 179 204)
   ;; :else Color/black
   ))

(defn water-color? [color]
  (and
   (or
    (= color (Color. 152 166 181))
    (= color (Color. 152 178 203))
    (= color (Color. 153 178 203))
    (= color (Color. 153 179 203))
    (= color (Color. 153 179 204))
    (= color (Color. 157 184 204))
    (= color (Color. 164 185 203))
    (= color (Color. 164 187 208))
    (= color (Color. 171 192 210))
    (= color (Color. 182 199 213))
    (= color (Color. 192 208 224))
    (= color (Color. 204 217 230))
    (= color (Color. 215 227 235)))
   color))

(defn clear [component]
  (let [bounds (.getBounds component) graphics (.getGraphics component)]
    (. graphics setColor (.getBackground component))
    (. graphics fillRect (.getX bounds) (.getY bounds) (.getWidth bounds) (.getHeight bounds))))

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_3BYTE_BGR)))

(defn render-static-map
  "Renders a static Google Map in the graphics context and returns the
  map as a buffered image."
  [component center & options]
  (let [map (apply static-map-image center options)]
    (. (.getGraphics component) drawImage map 0 0 nil)
    map))

(defn render-datatype [component datatype valid-time center & options]
  (let [graphics (.getGraphics component)
        map (apply render-static-map component center options)
        options (apply hash-map options)
        reader-fn (or (:reader options) interpolate-bilinear-2x2)
        zoom (or (:zoom options) (:zoom *options*))
        origin (location->coords center zoom)
        upper-left {:x (- (:x origin) (/ (.getWidth map) 2)) :y (- (:y origin) (/ (.getHeight map) 2))}
        offsets (coord-delta center (coords->location upper-left zoom) zoom)]
    (doseq [y (range 0 (.getHeight map)) x (range 0 (.getWidth map))]
      (let [location (coords->location {:x (+ x (:x origin) (:x offsets)) :y (+ y (:y origin) (:y offsets))} zoom)]
        (. graphics setColor
           (if (water-color? (Color. (. map getRGB x y)))
             (value->color (:value (reader-fn datatype valid-time location :nil 0)))
             (Color. (. map getRGB x y))))
        (. graphics fillRect x y 1 1)))))

(defn read-datatype-image [datatype valid-time center & options]
  (let [image (apply static-map-image center options)
        graphics (.getGraphics image)
        options (apply hash-map options)
        reader-fn (or (:reader options) interpolate-bilinear-2x2)
        zoom (or (:zoom options) (:zoom *options*))
        origin (location->coords center zoom)
        upper-left {:x (- (:x origin) (/ (.getWidth image) 2)) :y (- (:y origin) (/ (.getHeight image) 2))}
        offsets (coord-delta center (coords->location upper-left zoom) zoom)]
    (doseq [y (range 0 (.getHeight image)) x (range 0 (.getWidth image))]
      (let [location (coords->location {:x (+ x (:x origin) (:x offsets)) :y (+ y (:y origin) (:y offsets))} zoom)]
        (. graphics setColor
           (if (water-color? (Color. (. image getRGB x y)))
             (value->color (:value (reader-fn datatype valid-time location :nil 0)))
             (Color. (. image getRGB x y))))
        (. graphics fillRect x y 1 1)))
    image))

(defn save-datatype-image [filename datatype valid-time center & options]
  (let [image (apply read-datatype-image datatype valid-time center options)]
    (javax.imageio.ImageIO/write image (or (file-extension filename) "PNG") (java.io.File. filename))
    image))

(defn save-datatype-images [directory datatype center & options]
  (doseq [valid-time (valid-times datatype)]
    (println valid-time)
    (apply save-datatype-image
           (str directory "/" (str-time valid-time :basic-date-time-no-ms) ".png")
           datatype valid-time center options)))

;; (save-datatype-image "/tmp/test.png" *nww3* (nth (valid-times *nww3*) 5) (make-location 0 0))
;; (save-datatype-images "/tmp" *nww3* (make-location 0 110)  :width 640 :height 480 :zoom 2)

;; (display-formats)

;; (defn render-datatype [graphics datatype valid-time center & options]
;;   (let [map (apply render-static-map graphics center options)
;;         options (apply hash-map options)
;;         reader-fn (or (:reader options) interpolate-bilinear-2x2)
;;         zoom (or (:zoom options) (:zoom *options*))
;;         origin (location->coords center zoom)
;;         upper-left {:x (- (:x origin) (/ (.getWidth map) 2)) :y (- (:y origin) (/ (.getHeight map) 2))}
;;         offsets (coord-delta center (coords->location upper-left zoom) zoom)
;;         ]
;;     (doseq [y (range 0 (.getHeight map)) x (range 0 (.getWidth map))]
;;       (let [location (coords->location {:x (+ x (:x origin) (:x offsets)) :y (+ y (:y origin) (:y offsets))} zoom)]
;;         (if (water-color? (Color. (. map getRGB x y)))
;;           (let [data (reader-fn datatype valid-time location)]
;;             (. graphics setColor (value->color (:value data)))
;;             (. graphics fillRect x y 1 1)))))
;;     map))

(def *datatypes*
     (map #(open-datatype (apply make-datatype %))
          '(
            ("/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")
            ("/home/roman/.weather/20100215/akw.06.nc" "htsgwsfc")
            ("/home/roman/.weather/20100215/enp.06.nc" "htsgwsfc")
            ("/home/roman/.weather/20100215/nah.06.nc" "htsgwsfc")
            ("/home/roman/.weather/20100215/nph.06.nc" "htsgwsfc")
            ("/home/roman/.weather/20100215/wna.06.nc" "htsgwsfc")
            )))

;; (def *nww3* (nth *datatypes* 0))
;; (def *display* (create-display 300 300))
;; (clear *display*)

;; (render-static-map *display* (make-location 0 110) :width 400 :height 200 :zoom 2)
;; (render-datatype *display* *nww3* (nth (valid-times *nww3*) 5) (make-location 0 0) :zoom 3 :width 100 :height 100 :maptype "roadmap")
;; (render-datatype *display* *nww3* (nth (valid-times *nww3*) 5) (make-location 5 0) :zoom 1 :width 300 :height 300 :maptype "roadmap" :reader interpolate-bilinear-2x2)

;; (defmulti render-data 
;;   (fn [component data]
;;     (cond
;;      (isa? (class data) incanter.Matrix) :matrix
;;      (seq? data) :seq
;;      (vector? data) :vector)))

;; (defmethod render-data :matrix [component matrix]
;;   (let [graphics (.getGraphics component)
;;         width (:size (:longitude-axis (meta matrix)))
;;         height (:size (:latitude-axis (meta matrix)))]     
;;     (doseq [x (range width) y (range height) :let [value (sel matrix x y)] :when (not (.isNaN value))]
;;       (. graphics setColor (value->color value))
;;       (. graphics fillRect x (- (+ (* -1 y) height) 1) 1 1))))

;; (defmethod render-data :seq [component sequence]
;;   (render-data component (with-meta (apply vector sequence) (meta sequence))))

;; (defmethod render-data :vector [component vector]
;;   (let [graphics (.getGraphics component)
;;         width (:size (:longitude-axis (meta vector)))
;;         height (:size (:latitude-axis (meta vector)))]     
;;     (doseq [index (range (count vector))
;;             :let [value (nth vector index)
;;                   x (mod index width)
;;                   y (/ index width)]
;;             :when (not (.isNaN value))]      
;;       (. graphics setColor (value->color value))
;;       (. graphics fillRect x (+ (* -1 y) height) 1 1))))

;; (defn render-datatype [component datatype]
;;   (render-data component (read-seq datatype (first (valid-times datatype)))))

;; (defn render-datatypes [component & [datatypes]]
;;   (doseq [datatype datatypes] (render-datatype component datatype)))




;; (defn render-map [graphics center valid-time & options]
;;   (let [map (apply static-map-image center options)
;;         options (apply hash-map options)
;;         zoom (or (:zoom options) (:zoom *options*))
;;         origin (location->coords center zoom)
;;         upper-left {:x (- (:x origin) (/ (.getWidth map) 2)) :y (- (:y origin) (/ (.getHeight map) 2))}
;;         offsets (coord-delta center (coords->location upper-left zoom) zoom)
;;         ]
;;     (. graphics drawImage map 0 0 nil)
;;     (doseq [y (range 0 (.getHeight map)) x (range 0 (.getWidth map))]
;;       (let [location (coords->location {:x (+ x (:x origin) (:x offsets)) :y (+ y (:y origin) (:y offsets))} zoom)]
;;         (if (water-color? (Color. (. map getRGB x y)))
;;           (let [data (interpolate-bilinear-2x2 *datatype* valid-time location)]
;;             (. graphics setColor (value->color (:value data)))
;;             (. graphics fillRect x y 1 1)))))
;;     map))

;; (render-map (.getGraphics *display*) {:latitude 50 :longitude 0} (nth (valid-times *datatype*) 5) :zoom 1 :width 500 :height 400 :maptype "terrain")

;; (defn render-image [center valid-time & options]
;;   (let [image (apply static-map-image center options)
;;         graphics (.getGraphics image)
;;         options (apply hash-map options)
;;         zoom (or (:zoom options) (:zoom *options*))
;;         origin (location->coords center zoom)
;;         upper-left {:x (- (:x origin) (/ (.getWidth image) 2)) :y (- (:y origin) (/ (.getHeight image) 2))}
;;         offsets (coord-delta center (coords->location upper-left zoom) zoom)
;;         ]
;;     (doseq [y (range 0 (.getHeight image)) x (range 0 (.getWidth image))]
;;       (let [location (coords->location {:x (+ x (:x origin) (:x offsets)) :y (+ y (:y origin) (:y offsets))} zoom)]
;;         (if (water-color? (Color. (. image getRGB x y)))
;;           (let [data (read-at-location *datatype* valid-time location)]
;;             (. graphics setColor (value->color (:value data)))
;;             (. graphics fillRect x y 1 1)))))
;;     image))



;; (render-datatype (.getGraphics *display*) *datatype* (make-location 0 0) (nth (valid-times *datatype*) 5) :zoom 2 :width 500 :height 250 :maptype "roadmap")
;; (render-datatype (.getGraphics *display*) *nww3* (nth (valid-times *nww3*) 5) (make-location 0 0))


;; (clear *display*)
;; (render-map (.getGraphics *display*) (make-location 0 0) :width 500 :height 250)


;; (def *display* (create-display 500 250))
;; (clear *display*)
;; (render-map (.getGraphics *display*) {:latitude 0 :longitude 0} (nth (valid-times *datatype*) 5)
;;             :zoom 2 :width 500 :height 250 :maptype "roadmap")

;; (render-datatype *display* *datatype*)
;; ;; (*datatype*)

;; (def *display* (create-display 500 300))
;; (clear *display*)
;; (time (render-datatype *display* *datatype*))
;; (clear *display*)
;; (render-data *display* (read-matrix *datatype* (first (valid-times *datatype*))))
;; (render-data *display* (read-seq *datatype* (first (valid-times *datatype*))))

;; (interpolate-bilinear *datatype* (first (valid-times *datatype*)) (make-location 0 0))

;; (take 5 (render-map *display* {:latitude 0 :longitude 0} :zoom 2 :width 200 :height 100))
;; (nth (render-map *display* {:latitude 0 :longitude 0} :zoom 2 :width 360 :height 180) 359)
;; (nth (render-map *display* {:latitude 0 :longitude 0} :zoom 2 :width 360 :height 180) 1)

;; (x-coord-delta -180 180 0)
;; (longitude-delta -180 180 0)
;; (latitude-delta -180 180 0)


;; (def *map* (static-map-image (make-location 0 0) :width 500 :height 400 :zoom 2))
;; (. (.getGraphics *display*) drawImage *map* 0 0 nil)
;; (. (.getGraphics *display*) drawImage (remove-water *map*) 0 0 nil)

;; (def *data* (read-datatype *datatype* (first (valid-times *datatype*))))

;; (time (render-datatypes *display* *datatypes*))

;; (defn remove-water [source]
;;   (let [target (make-buffered-image (.getWidth source) (.getHeight source))
;;         graphics (.getGraphics target)]
;;     (. graphics drawImage source 0 0 nil)
;;     (. graphics setColor Color/black)
;;     (doseq [x (range 0 (.getWidth source)) y (range 0 (.getHeight source))]
;;       (if (= (Color. (. target getRGB x y)) (Color. 153 179 204))
;;         (. graphics fillRect x y 1 1)))
;;     target))
