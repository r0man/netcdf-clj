(defproject netcdf-clj "0.0.2-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :url "http://github.com/r0man/netcdf-clj"
  :dependencies [[clj-time "0.1.0-SNAPSHOT"]
                 [commons-httpclient "3.1"]
                 [essi-unidata/netcdf-java "4.0.41"]
                 [google-maps "0.2-SNAPSHOT"]
                 [incanter/incanter-core "1.2.3-SNAPSHOT"]
                 [javax.media/jai_core "1.1.3"]
                 [opendap "2.1"]
                 [org.clojure/clojure "1.2.0-RC1"]
                 [org.clojure/clojure-contrib "1.2.0-RC1"]
                 [org.slf4j/slf4j-log4j12 "1.3.1"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [swank-clojure "1.2.1"]]
  :repositories {"geosolutions" "http://mvn.geo-solutions.it"})
