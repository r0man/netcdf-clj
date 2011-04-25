(defproject netcdf-clj "0.0.2-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :url "http://github.com/r0man/netcdf-clj"
  :dependencies [[clj-time "0.3.0"]
                 [commons-httpclient "3.1"]
                 [essi-unidata/netcdf-java "4.0.41"]
                 [google-maps "0.4.1-SNAPSHOT"]
                 [incanter/incanter-core "1.2.3"]
                 [javax.media/jai_core "1.1.3"]
                 [opendap/opendap "2.2"]
                 [org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.slf4j/slf4j-log4j12 "1.5.6"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :tasks [netcdf.tasks]
  :repositories {"unifi.it" "http://ulisse.pin.unifi.it:8081/nexus/content/groups/open.repos/"})
