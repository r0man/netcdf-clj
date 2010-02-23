(defproject netcdf-clj "0.0.1-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :url "http://github.com/r0man/netcdf-clj"
  :dependencies [[commons-httpclient "3.1"]
                 [google-maps "0.0.1-SNAPSHOT"]
                 [essi-unidata/netcdf-java "4.0.41"]
                 [opendap "2.1"]
                 [org.slf4j/slf4j-log4j12 "1.3.1"]
                 
                 [org.incanter/incanter-full "1.2.0-SNAPSHOT"]
                 [org.apache.maven/maven-ant-tasks "2.0.10"]
                 [org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.0-SNAPSHOT"]]
  :dev-dependencies [[autodoc "0.7.0"]
                     [lein-clojars "0.5.0-SNAPSHOT"]
                     [swank-clojure "1.1.0"]]
  :repositories [["geosolutions" "http://mvn.geo-solutions.it"]])
