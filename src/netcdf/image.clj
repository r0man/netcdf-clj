(ns netcdf.image
  (:import java.awt.image.BufferedImage
           ucar.nc2.dt.grid.GeoGrid))

(defn make-buffered-image [width height & [type]]  
  (BufferedImage. width height (or type BufferedImage/TYPE_INT_ARGB)))
