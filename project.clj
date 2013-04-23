(defproject netcdf-clj "0.0.6-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :min-lein-version "2.0.0"
  :url "http://github.com/r0man/netcdf-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-time "0.5.0"]
                 [commandline-clj "0.1.5"]
                 [digest "1.4.3"]
                 [edu.ucar/netcdf "4.3.16"]
                 [edu.ucar/opendap "4.3.16"]
                 [incanter/incanter-core "1.4.1"]
                 [javax.media/jai-core "1.1.3"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/core.memoize "0.5.3"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/tools.logging "0.2.6"]]
  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.0.11"]
                                  [ch.qos.logback/logback-core "1.0.11"]]
                   :resource-paths ["test-resources"]}}
  :repositories {"jboss-third-party" "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
                 "unidata" "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases"})
