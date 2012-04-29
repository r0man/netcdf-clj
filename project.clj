(defproject netcdf-clj/netcdf-clj "0.0.4-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :min-lein-version "2.0.0"
  :url "http://github.com/r0man/netcdf-clj"
  :dependencies [[clj-time "0.4.2"]
                 [commandline-clj "0.1.2"]
                 ;; [commons-httpclient "3.1"]
                 [digest "1.3.0"]
                 [edu.ucar/netcdf "4.2.32" :exclusions [org.slf4j/slf4j-api]]
                 [edu.ucar/opendap "4.2.32"]
                 [geocoder-clj "0.0.6-SNAPSHOT"]
                 [google-maps "0.5.1-SNAPSHOT"]
                 [incanter/incanter-core "1.2.4"]
                 [javax.media/jai_core "1.1.3"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.slf4j/slf4j-log4j12 "1.6.1"]]
  :run-aliases {:forecast netcdf.main/-main}
  :profiles {:dev {:resource-paths ["test-resources"]}}
  :repositories {"geotoolkit" "http://maven.geotoolkit.org"}
  :aot [netcdf.forecast netcdf.main])
