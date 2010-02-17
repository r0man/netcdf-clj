(ns netcdf.render
  (:import java.awt.Color java.awt.image.BufferedImage)
  (:use netcdf.datatype))

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

(render-datatype *component* *data*)

(defn fill-component [component & [color]]
  (let [bounds (.getBounds component) graphics (.getGraphics component)]
    (if color (. graphics setColor color))
    (. graphics fillRect (.getX bounds) (.getY bounds) (.getWidth bounds) (.getHeight bounds))))

(defn clear-component [component]
  (fill-component component Color/black))

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_3BYTE_BGR)))

(defn datatype-component [width height]
  (javax.swing.JLabel. (javax.swing.ImageIcon. (make-buffered-image width height))))

(defn render-datatype [component data]  
  (clear-component component)
  (let [graphics (.getGraphics component)]
    (doseq [{:keys [actual-location value]} data]
      (let [point (location->point actual-location)]
        (. graphics setColor (value->color value))
        (. graphics fillRect (:x point) (:y point) 1 1)))
    graphics))

(defn display-datatype [width height & [data]]
  (let [component (datatype-component width height)]
    (doto (javax.swing.JFrame.)
      (.setSize (+ width 6) (+ height 24))
      (.add component)
      (.setVisible true))
    (if data (render-datatype component data))
    component))

(def *datatype* (open-datatype (make-datatype "/home/roman/.weather/20100215/akw.06.nc" "htsgwsfc")))
(def *datatype* (open-datatype (make-datatype "/home/roman/.weather/20100215/nww3.06.nc" "htsgwsfc")))

(def *data* (read-datatype *datatype* (first (valid-times *datatype*))))

(def *component* (display-datatype 360 180 *data*))
;; (clear-component *component*)
(render-datatype *component* *data*)

