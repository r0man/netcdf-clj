(defproject netcdf-clj "0.0.6-SNAPSHOT"
  :description "Clojure NetCDF Library."
  :min-lein-version "2.0.0"
  :url "http://github.com/r0man/netcdf-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cascalog "2.1.1"]
                 [clj-time "0.6.0"]
                 [commandline-clj "0.1.6"]
                 [digest "1.4.3"]
                 [edu.ucar/netcdf "4.3.18"]
                 [edu.ucar/opendap "4.3.16"]
                 [edu.ucar/unidataCommon "4.2.20"]
                 [incanter/incanter-core "1.4.1"]
                 [javax.media/jai-core "1.1.3"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/tools.logging "0.2.6"]]
  :plugins [[lein-junit "1.1.3"]]
  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.0.13"]
                                  [ch.qos.logback/logback-core "1.0.13"]]
                   :resource-paths ["test-resources"]}
             :provided {:dependencies [[org.apache.hadoop/hadoop-core "2.0.0-mr1-cdh4.5.0"]
                                       [org.apache.hadoop/hadoop-client "2.0.0-cdh4.5.0"]
                                       [org.apache.hadoop/hadoop-common "2.0.0-cdh4.5.0"]]}}
  :deploy-repositories [["releases" :clojars]]
  :repositories {"cloudera" "https://repository.cloudera.com/artifactory/cloudera-repos"
                 "jboss-third-party" "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
                 "unidata" "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases"}
  :javac-options ["-target" "1.6" "-source" "1.6" "-g"]
  :java-source-paths ["src" "test"]
  :junit ["test"])
