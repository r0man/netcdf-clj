(defproject netcdf-clj "0.0.14"
  :description "Clojure NetCDF Library."
  :min-lein-version "2.0.0"
  :url "http://github.com/r0man/netcdf-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cascalog "2.1.1"]
                 [clj-time "0.12.2"]
                 [digest "1.4.5"]
                 [edu.ucar/netcdf4 "4.6.6"]
                 [edu.ucar/opendap "4.6.6"]
                 [edu.ucar/unidataCommon "4.2.20"]
                 [incanter/incanter-core "1.4.1"]
                 [javax.media/jai-core "1.1.3"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.memoize "0.5.9"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/tools.logging "0.3.1"]]
  :plugins [[lein-junit "1.1.8"]]
  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.1.8"]
                                  [ch.qos.logback/logback-core "1.1.8"]]
                   :resource-paths ["test-resources"]}
             :provided {:dependencies [[org.apache.hadoop/hadoop-core "2.6.0-mr1-cdh5.10.1"]
                                       [org.apache.hadoop/hadoop-client "2.7.3"]
                                       [org.apache.hadoop/hadoop-common "2.7.3"]]}}
  :deploy-repositories [["releases" :clojars]]
  :repositories {"cloudera" "https://repository.cloudera.com/artifactory/cloudera-repos"
                 "jboss-third-party" "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
                 "unidata" "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases"}
  :javac-options ["-target" "1.6" "-source" "1.6" "-g"]
  :java-source-paths ["src" "test"]
  :junit ["test"])
