(defproject netcdf-clj/netcdf-clj "0.0.5-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :min-lein-version "2.0.0"
  :url "http://github.com/r0man/netcdf-clj"
  :dependencies [[clj-time "0.4.4"]
                 [commandline-clj "0.1.3"]
                 [digest "1.4.2"]
                 [edu.ucar/netcdf "4.3.15"]
                 [edu.ucar/opendap "4.3.15"]
                 [geocoder-clj "0.1.0"]
                 [incanter/incanter-core "1.4.1"]
                 [javax.media.jai/com.springsource.javax.media.jai.core "1.1.3"]
                 [org.clojure/clojure "1.5.0"]
                 [org.clojure/core.memoize "0.5.2"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/tools.logging "0.2.3"]]
  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.0.7"]
                                  [ch.qos.logback/logback-core "1.0.7"]]
                   :resource-paths ["test-resources"]}}
  :repositories {"springsource" "http://repository.springsource.com/maven/bundles/external"
                 "unidata" "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases"})
