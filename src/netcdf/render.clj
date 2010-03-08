(ns netcdf.render
  (:import java.awt.image.BufferedImage
           java.io.File
           javax.imageio.ImageIO           
           incanter.Matrix
           (java.awt Color Dimension)
           (java.awt.event KeyListener)
           (javax.swing JFrame JOptionPane JPanel))
  (:use [clojure.contrib.seq-utils :only (flatten)]
        netcdf.datatype
        netcdf.interpolation
        netcdf.location
        netcdf.utils
        google.maps.static
        google.maps.projection
        incanter.core
        incanter.chrono))

(def *render-options* {:center (make-location 0 0) :zoom 2 :width 512 :height 256})

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
  (BufferedImage. width height (or type BufferedImage/TYPE_INT_ARGB)))

(defn locations [center width height zoom]
  (let [center (if (map? center) center {:latitude (latitude center) :longitude (longitude center)})
        origin (location->coords center zoom)
        upper-left {:x (- (:x origin) (/ width 2)) :y (- (:y origin) (/ height 2))}
        offsets (coord-delta center (coords->location upper-left zoom) zoom)]
    (for [y (range 0 height) x (range 0 width)]
      {:x x :y y :location (coords->location {:x (+ x (:x origin) (:x offsets)) :y (+ y (:y origin) (:y offsets))} zoom)})))

(defn render-static-map
  "Renders a static Google Map in the graphics context and returns the
  map as a buffered image."
  [graphics center & options]
  (let [map (apply static-map-image {:latitude (latitude center) :longitude (longitude center)} options)]
    (. graphics drawImage map 0 0 nil)))

(defn render-datatype [graphics datatype & options]
  (let [options (merge *render-options* (apply hash-map options))
        center (:center options)
        width (:width options)
        height (:height options)
        zoom (:zoom options)
        map (static-map-image {:latitude (latitude center) :longitude (longitude center)} :width width :height height :zoom zoom)]
    (doseq [{:keys [x y location]} (locations center width height zoom)]
      (if (water-color? (Color. (. map getRGB x y))) 
        (let [value (:value (interpolate-datapoint datatype (make-location (:latitude location) (:longitude location))))]
          (. graphics setColor (value->color value))         
          (. graphics fillRect x y 1 1))))))

(defn datatype-image
  "Render the datatype into a new image."
  [datatype & options]
  (let [options (merge *render-options* (apply hash-map options))
        image (make-buffered-image (:width options) (:height options))]
    (apply render-datatype (.getGraphics image) datatype (flatten (seq options)))
    image))

(defn write-datatype-image
  "Render the datatype and write the image to the target."
  [target datatype & options]
  (let [format (or (:format (apply hash-map options)) (file-extension target) "PNG")
        image (apply datatype-image datatype options)]
    (ImageIO/write image format target)
    target))

(defn save-datatype-image
  "Render the datatype and save the image to filename."
  [filename datatype & options]
  (apply write-datatype-image (File. filename) datatype options)
  filename)

(defn save-datatype-images [directory datatype & options]
  (for [valid-time (valid-times datatype)]
    (let [filename (str directory "/" (str-time valid-time :basic-date-time-no-ms) ".png")
          matrix (read-matrix datatype :valid-time valid-time)]
      (apply save-datatype-image filename matrix options))))

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
;; (def *matrix* (read-matrix *nww3*))
;; (def *display* (create-display (:width *render-options*) (:height *render-options*)))

;; (clear *display*)
;; (render-static-map (.getGraphics *display*) (:center *render-options*) :zoom (:zoom *render-options*) :width (.getWidth *display*) :height (.getHeight *display*))
;; (time (render-datatype (.getGraphics *display*) *matrix* :center (:center *render-options*) :zoom (:zoom *render-options*) :width (.getWidth *display*) :height (.getHeight *display*)))

;; (save-datatype-image "/tmp/test.png" *matrix* :zoom 2 :center (make-location 0 100) :width 10 :height 10)
;; (save-datatype-images "/tmp" *nww3* :zoom 2 :center (make-location 0 100) :width 512 :height 256)

