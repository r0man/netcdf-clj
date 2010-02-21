(ns netcdf.render
  (:import java.awt.image.BufferedImage
           (java.awt Color Dimension)
           (java.awt.event KeyListener)
           (javax.swing JFrame JOptionPane JPanel))
  (:use netcdf.datatype google.maps.static google.maps.projection netcdf.location))

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
  ([width height]
     (configure-display (JFrame.) (create-panel width height))))

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
   :else Color/black))

;; (defn location->point [location]
;;   {:x (+ (:longitude location) 180)
;;    :y (+ (:latitude location) 90)})

(defn location->point [location]
  {:x (* 1(+ (:longitude location) 180))
   :y (* 1 (+ (* (+ (:latitude location) 90) -1) 180))})

(defn clear [component]
  (let [bounds (.getBounds component) graphics (.getGraphics component)]
    (println (.getBackground component))
    (. graphics setColor (.getBackground component))
    (. graphics fillRect (.getX bounds) (.getY bounds) (.getWidth bounds) (.getHeight bounds))))

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_3BYTE_BGR)))

(defn render-data [component data]  
;  (clear component)
  (let [graphics (.getGraphics component)]
    (doseq [{:keys [actual-location value]} data :when (not (.isNaN value))]
      (let [point (location->point actual-location)]
        (. graphics setColor (value->color value))
        (. graphics fillRect (:x point) (:y point) 1 1)))
    graphics))

(defn render-datatype [component datatype]
  (render-data component (read-datatype datatype (first (valid-times datatype)))))

(defn render-datatypes [component & [datatypes]]
  (doseq [datatype datatypes] (render-datatype component datatype)))

(defn remove-water [source]
  (let [target (make-buffered-image (.getWidth source) (.getHeight source))
        graphics (.getGraphics target)]
    (. graphics drawImage source 0 0 nil)
    (. graphics setColor Color/black)
    (doseq [x (range 0 (.getWidth source)) y (range 0 (.getHeight source))]
      (if (= (Color. (. target getRGB x y)) (Color. 152 178 203))
        (. graphics fillRect x y 1 1)))
    target))

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

(*datatype*)

(def *display* (create-display))
(clear *display*)
(time (render-datatype *display* *datatype*))

(x-coord-delta -180 180 0)
(longitude-delta -180 180 0)
(latitude-delta -180 180 0)


(def *map* (static-map-image (make-location 0 0) :width 360 :height 180 :zoom 0))
(. (.getGraphics *display*) drawImage *map* 0 0 nil)
(. (.getGraphics *display*) drawImage (remove-water *map*) 0 0 nil)

;; (def *data* (read-datatype *datatype* (first (valid-times *datatype*))))

;; (time (render-datatypes *display* *datatypes*))

