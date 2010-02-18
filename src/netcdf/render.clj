(ns netcdf.render
  (:import java.awt.image.BufferedImage
           (java.awt Color Dimension)
           (java.awt.event KeyListener)
           (javax.swing JFrame JOptionPane JPanel))
  (:use netcdf.datatype netcdf.map netcdf.location))

(defn create-panel [width height]
  (proxy [JPanel KeyListener] [] 
    (getPreferredSize [] (Dimension. width height))
    (keyPressed [e])
    (keyReleased [e]) 
    (keyTyped [e])))

(defn configure-display [frame panel]
  (doto panel
    (.setFocusable true)
    (.addKeyListener panel))
  (doto frame
    (.add panel)
    (.pack)
    (.setVisible true))
  panel)

(defn create-display
  ([] (configure-display 360 180))
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
   :else Color/black))

;; (defn location->point [location]
;;   {:x (+ (:longitude location) 180)
;;    :y (+ (:latitude location) 90)})

(defn location->point [location]
  {:x (+ (:longitude location) 180)
   :y (+ (* (+ (:latitude location) 90) -1) 180)})

(defn fill-component [component & [color]]
  (let [bounds (.getBounds component) graphics (.getGraphics component)]
    (if color (. graphics setColor color))
    (. graphics fillRect (.getX bounds) (.getY bounds) (.getWidth bounds) (.getHeight bounds))))

(defn clear-component [component]
  (fill-component component Color/black))

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_3BYTE_BGR)))

(defn render-datatype [component data]  
  (clear-component component)
  (let [graphics (.getGraphics component)]
    (doseq [{:keys [actual-location value]} data]
      (let [point (location->point actual-location)]
        (. graphics setColor (value->color value))
        (. graphics fillRect (:x point) (:y point) 1 1)))
    graphics))

(def *display* (create-display))

(def *datatype* (open-datatype (make-datatype "/home/roman/.weather/20100215/akw.06.nc" "htsgwsfc")))
(def *datatype* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))

(def *data* (read-datatype *datatype* (first (valid-times *datatype*))))
(count *data*)
 
(render-datatype *display* *data*)
(clear-component *display*)

;; (def *component* (display-datatype 360 180 *data*))
;; (clear-component *component*)
;; (def *map* (static-map-image (make-location 0 0) :width 360 :height 180))
;; (display-component
;;  (doto (javax.swing.JLabel. (javax.swing.ImageIcon. *map*))
;;    (.setSize 360 180)))



