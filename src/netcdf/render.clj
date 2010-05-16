(ns netcdf.render
  (:import java.awt.image.BufferedImage
           java.awt.GraphicsEnvironment
           java.awt.RenderingHints
           java.awt.geom.AffineTransform
           java.io.File
           javax.imageio.ImageIO           
           incanter.Matrix
           (java.awt Color Dimension)
           (java.awt.event KeyListener)
           (javax.swing JFrame JOptionPane JPanel))
  (:use [clojure.contrib.seq-utils :only (flatten includes?)]
        [clj-time.format :only (formatters show-formatters unparse)]
        netcdf.datatype
        netcdf.interpolation
        netcdf.location
        netcdf.utils
        clojure.contrib.profile
        google.maps.static
        google.maps.projection
        incanter.core))

(def *render-options* {:center (make-location 0 0) :zoom 4 :width 512 :height 256})

(def *water-colors*
     [(Color. 152 166 181)
      (Color. 152 178 203)
      (Color. 153 178 203)
      (Color. 153 179 203)
      (Color. 153 179 204)
      (Color. 157 184 204)
      (Color. 164 185 203)
      (Color. 164 187 208)
      (Color. 171 192 210)
      (Color. 182 199 213)
      (Color. 192 208 224)
      (Color. 204 217 230)
      (Color. 215 227 235)])

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

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_INT_ARGB)))

(defn matrix->image [matrix color-fn]
  (let [image (make-buffered-image (ncol matrix) (nrow matrix)) graphics (.getGraphics image)]
    (doseq [row (range 0 (nrow matrix)) col (range 0 (ncol matrix))]
      (. graphics setColor (color-fn (sel matrix col row)))
      (. graphics fillRect row col 1 1))
    image))

(defn graphics-configuration []
  (-> (GraphicsEnvironment/getLocalGraphicsEnvironment)
      (.getDefaultScreenDevice)
      (.getDefaultConfiguration)))

(defn create-compatible-image [image width height]
  (let [color-model (.getColorModel image)]
    (BufferedImage. color-model (. color-model createCompatibleWritableRaster width height) (.isAlphaPremultiplied color-model) nil)))

(defn value->color [#^Double value]
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
  (and (includes? *water-colors* color) color))

(defn clear [component]
  (let [bounds (.getBounds component) graphics (.getGraphics component)]
    (. graphics setColor (.getBackground component))
    (. graphics fillRect (.getX bounds) (.getY bounds) (.getWidth bounds) (.getHeight bounds))))

(defn image-coords->location [center #^Integer x #^Integer y #^Integer width #^Integer height #^Integer zoom]
  (let [center-xy (location->coords (location->map center) zoom)]
    (coords->location
     {:x (+ x (:x center-xy) (/ width -2))
      :y (+ y (:y center-xy) (/ height -2))} zoom)))

(defn render-image
  [graphics image & options]
  (. graphics drawImage image 0 0 nil))

(defn render-static-map
  "Renders a static Google Map in the graphics context and returns the
  map as a buffered image."
  [graphics center & options]
  (let [map (apply static-map-image {:latitude (latitude center) :longitude (longitude center)} options)]
    (render-image graphics map)))

(defn render-datatype [graphics datatype & options]
  (let [{:keys [center width height valid-time zoom]} (merge *render-options* (apply hash-map options))
        map (static-map-image (location->map center) :width width :height height :zoom zoom)]
    (doseq [y (range 0 (int height)) x (range 0 (int width))]
      (if (water-color? (Color. (. map getRGB x y))) 
        (let [value (read-datapoint datatype (image-coords->location center x y width height zoom) :valid-time valid-time)]
          (. graphics setColor (value->color value))         
          (. graphics fillRect x y 1 1)
          )))))

(defn resize-image [source width height interpolation]
  (let [x-scale (/ width (.getWidth source))
        y-scale (/ height (.getHeight source))
        target (create-compatible-image source width height)]  
    (doto (.createGraphics target)
      (.setRenderingHint RenderingHints/KEY_INTERPOLATION interpolation)
      (.drawRenderedImage source (. AffineTransform getScaleInstance x-scale y-scale)))
    target))

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
    (let [filename (str directory "/" (unparse (formatters :basic-date-time-no-ms) valid-time) ".png")
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

;; (nrow *matrix*)
;; (ncol *matrix*)

;; (resize-image *image* 300 300 RenderingHints/VALUE_INTERPOLATION_BICUBIC)
;; (render-image (.getGraphics *display*) (resize-image *image* 300 300 RenderingHints/VALUE_INTERPOLATION_BILINEAR))
;; (render-image (.getGraphics *display*) *image*)

;; (def *image* (matrix->image *matrix* value->color))
;; (clear *display*)
;; (render-image (.getGraphics *display*) (matrix->image *matrix* value->color))

;; (. (.getGraphics *display*) drawImage (matrix->image *matrix* value->color) 0 0 nil)
;; (. (.getGraphics *display*) drawImage (matrix->image *matrix* value->color) 0 0 nil)

;; (render-image (.getGraphics *display*) (matrix->image *matrix* value->color))

;; (time
;;  (matrix->image *matrix* value->color))

;; (interpolate-datapoint *matrix* (make-location 0 0) :valid-time (nth (valid-times *nww3*) 10))

;; (clear *display*)
;; (render-static-map (.getGraphics *display*) (:center *render-options*) :zoom (:zoom *render-options*) :width (.getWidth *display*) :height (.getHeight *display*))
;; (time (render-datatype (.getGraphics *display*) *matrix*))
;; (take 100 (render-datatype (.getGraphics *display*) *matrix*))

 ;; (time (render-datatype (.getGraphics *display*) *matrix* :center (:center *render-options*) :zoom (:zoom *render-options*) :width (.getWidth *display*) :height (.getHeight *display*))))


;; (save-datatype-image "/tmp/test.png" *matrix* :zoom 2 :center (make-location 0 100) :width 10 :height 10)
;; (save-datatype-images "/tmp" *nww3* :zoom 2 :center (make-location 0 100) :width 512 :height 256)

