(ns netcdf.render
  (:import java.awt.image.BufferedImage
           (java.awt Color Dimension)
           (java.awt.event KeyListener)
           (javax.swing JFrame JOptionPane JPanel))
  (:use netcdf.datatype
        google.maps.static
        google.maps.projection
        incanter.core
        netcdf.location))

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
  (or (= color (Color. 152 178 203))
      (= color (Color. 153 179 203))
      (= color (Color. 153 179 204))))

(defn location->point [location]
  {:x (+ (:longitude location) 180)
   :y (+ (* (+ (:latitude location) 90) -1) 180)})

(defn clear [component]
  (let [bounds (.getBounds component) graphics (.getGraphics component)]
    (println (.getBackground component))
    (. graphics setColor (.getBackground component))
    (. graphics fillRect (.getX bounds) (.getY bounds) (.getWidth bounds) (.getHeight bounds))))

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_3BYTE_BGR)))

(defmulti render-data 
  (fn [component data]
    (cond
     (isa? (class data) incanter.Matrix) :matrix
     (seq? data) :seq
     (vector? data) :vector)))

(defmethod render-data :matrix [component matrix]
  (let [graphics (.getGraphics component)
        width (:size (:longitude-axis (meta matrix)))
        height (:size (:latitude-axis (meta matrix)))]     
    (doseq [x (range width) y (range height) :let [value (sel matrix x y)] :when (not (.isNaN value))]
      (. graphics setColor (value->color value))
      (. graphics fillRect x (- (+ (* -1 y) height) 1) 1 1))))

(defmethod render-data :seq [component sequence]
  (render-data component (with-meta (apply vector sequence) (meta sequence))))

(defmethod render-data :vector [component vector]
  (let [graphics (.getGraphics component)
        width (:size (:longitude-axis (meta vector)))
        height (:size (:latitude-axis (meta vector)))]     
    (doseq [index (range (count vector))
            :let [value (nth vector index)
                  x (mod index width)
                  y (/ index width)]
            :when (not (.isNaN value))]      
      (. graphics setColor (value->color value))
      (. graphics fillRect x (+ (* -1 y) height) 1 1))))

(defn render-datatype [component datatype]
  (render-data component (read-seq datatype (first (valid-times datatype)))))

(defn render-datatypes [component & [datatypes]]
  (doseq [datatype datatypes] (render-datatype component datatype)))

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

(def *datatype* (nth *datatypes* 0))

(defn render-map [display center valid-time & options]
  (let [map (apply static-map-image (make-location 0 0) options)
        graphics (.getGraphics display)
        options (apply hash-map options)
        zoom (or (:zoom options) (:zoom *options*))
        origin (location->coords center zoom)
        upper-left {:x (- (:x origin) (/ (.getWidth map) 2)) :y (- (:y origin) (/ (.getHeight map) 2))}
        x-offset (x-coord-delta (:longitude center) (:longitude (coords->location upper-left zoom)) zoom)
        y-offset (y-coord-delta (:latitude center) (:latitude (coords->location upper-left zoom)) zoom)
        ]
    (. graphics drawImage map 0 0 nil)
    (doseq [y (range 0 (.getHeight map)) x (range 0 (.getWidth map))]
      (let [location (coords->location {:x (+ (:x origin) x x-offset) :y (+ (:y origin) y y-offset)} zoom)]
        (if (water-color? (Color. (. map getRGB x y)))
          (let [data (read-at-location *datatype* valid-time location)]
            (. graphics setColor (value->color (:value data)))
            (. graphics fillRect x y 1 1)))))))

;; (render-datatype *display* *datatype*)
;; ;; (*datatype*)

;; (def *display* (create-display 500 300))
;; ;; (clear *display*)
;; ;; (time (render-datatype *display* *datatype*))
;; (clear *display*)
;; (render-data *display* (read-matrix *datatype* (first (valid-times *datatype*))))
;; (render-data *display* (read-seq *datatype* (first (valid-times *datatype*))))


;; (water-color? (Color. 242 239 233))



;; (def *display* (create-display 500 400))
;; (clear *display*)
;; (render-map *display* {:latitude 0 :longitude 0} (nth (valid-times *datatype*) 5) :zoom 1 :width 500 :height 400)

;; (take 5 (render-map *display* {:latitude 0 :longitude 0} :zoom 2 :width 200 :height 100))
;; (nth (render-map *display* {:latitude 0 :longitude 0} :zoom 2 :width 360 :height 180) 359)
;; (nth (render-map *display* {:latitude 0 :longitude 0} :zoom 2 :width 360 :height 180) 1)

;; (x-coord-delta -180 180 0)
;; (longitude-delta -180 180 0)
;; (latitude-delta -180 180 0)


;; (def *map* (static-map-image (make-location 0 0) :width 360 :height 180 :zoom 0))
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
