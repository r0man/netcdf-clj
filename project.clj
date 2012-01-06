(defproject netcdf-clj "0.0.3-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :url "http://github.com/r0man/netcdf-clj"
  :aot [netcdf.forecast
        netcdf.main]
  :dependencies [[clj-time "0.3.3"]
                 [commandline-clj "0.1.0-SNAPSHOT"]
                 [commons-httpclient "3.1"]
                 [digest "1.3.0"]
                 [edu.ucar/netcdf "4.2.26"]
                 [edu.ucar/opendap "4.2.26"]
                 [geocoder-clj "0.0.4-SNAPSHOT"]
                 [google-maps "0.5.1-SNAPSHOT"]
                 [incanter/incanter-core "1.2.4"]
                 [javax.media/jai_core "1.1.3"]
                 [org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/tools.cli "0.1.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.slf4j/slf4j-log4j12 "1.5.6"]]
  :repositories {"mbari" "http://mbari-maven-repository.googlecode.com/svn/repository"}
  :run-aliases {:forecast netcdf.main/-main}
  ;; :warn-on-reflection true
  )
