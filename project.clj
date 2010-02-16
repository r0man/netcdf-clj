(defproject netcdf "0.3-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :url "http://github.com/r0man/netcdf-clj"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.0-SNAPSHOT"]
                 [org.apache.maven/maven-ant-tasks "2.0.10"]
                 [essi-unidata/netcdf-java "4.0.41"]]
  :dev-dependencies [[autodoc "0.7.0"]
                     [lein-clojars "0.5.0-SNAPSHOT"]
                     [swank-clojure "1.1.0"]]
  :repositories [["essi-unidata" "http://cropwiki.irri.org/m2repo"]])
