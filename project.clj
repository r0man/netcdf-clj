(defproject netcdf-clj "0.0.1-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :url "http://github.com/r0man/netcdf-clj"
  :dependencies [[commons-httpclient "3.1"]
                 [google-maps "0.0.1-SNAPSHOT"]
                 [essi-unidata/netcdf-java "4.0.41"]
                 [org.apache.maven/maven-ant-tasks "2.0.10"]
                 [org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.0-SNAPSHOT"]
                 [org.slf4j/slf4j-log4j12 "1.3.1"]]
  :dev-dependencies [[autodoc "0.7.0"]
                     [lein-clojars "0.5.0-SNAPSHOT"]
                     [swank-clojure "1.1.0"]]
  :repositories [["essi-unidata" "http://cropwiki.irri.org/m2repo"]])
