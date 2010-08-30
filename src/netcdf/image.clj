(ns netcdf.image
  (:import java.awt.image.BufferedImage
           java.io.File
           javax.imageio.ImageIO           
           (java.awt Color Dimension)
           (java.awt.event KeyListener)
           (javax.swing JFrame JOptionPane JPanel))
  (use netcdf.utils))

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_INT_ARGB)))

(defn write-buffered-image
  "Write the buffered image to filename."
  [image filename & {:keys [format]}]
  (let [format (or format (file-extension filename) "PNG")]
    (ImageIO/write image format (File. filename))
    image))

;; (defn create-display
;;   ([] (create-display 360 180))
;;   ([width height]
;;      (configure-display (JFrame.) (create-panel width height))))

(defn create-frame [& {:keys [width height]}]
  (doto (JFrame.)
    (.setSize (or width 300) (or height 200))
    (.setVisible true)))

(defn create-panel [& {:keys [background width height]}]
  (let [panel (proxy [JPanel KeyListener] [] 
                (getPreferredSize [] (Dimension. (or width 300) (or height 200)))
                (keyPressed [e])
                (keyReleased [e]) 
                (keyTyped [e]))]
    (doto panel
      (.setBackground (or background Color/black))
      (.setFocusable true)
      (.addKeyListener panel))))

;; (create-panel)
;; (def *display* (create-display))

;; (def *frame* (doto (JFrame.) (.setVisible true)))
;; (def *panel* (create-panel 288 157))
;; (.add *frame* *panel*)
;; (.pack *frame*)

;; (.add *panel* (javax.swing.ImageIcon.))
;; (.removeAll *frame*)
;; (.removeAll (.getRootPane *frame*))

;; (.getWidth *frame*)
;; (.getHeight *frame*)

;; (.setSize *frame* 500 200)

;; *display*
