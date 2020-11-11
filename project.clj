(defproject netcdf-clj "0.0.18"
  :description "Clojure NetCDF Library."
  :min-lein-version "2.0.0"
  :url "https://github.com/r0man/netcdf-clj"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-time "0.15.2"]
                 [digest "1.4.9"]
                 [edu.ucar/netcdf4 "4.6.13"]
                 [edu.ucar/opendap "4.6.13"]
                 [edu.ucar/unidataCommon "4.2.20"]
                 [javax.media/jai-core "1.1.3"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/core.memoize "1.0.236"]
                 [org.clojure/data.zip "1.0.0"]
                 [org.clojure/tools.logging "1.1.0"]]
  :plugins [[lein-junit "1.1.9"]]
  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                                  [ch.qos.logback/logback-core "1.2.3"]
                                  [junit "4.13.1"]]
                   :java-source-paths ["test"]
                   :resource-paths ["test-resources"]}
             :provided {:dependencies [[cascalog "2.1.1"]
                                       [org.apache.hadoop/hadoop-core "2.6.0-mr1-cdh5.16.99"]
                                       [org.apache.hadoop/hadoop-client "3.3.0"]
                                       [org.apache.hadoop/hadoop-common "3.3.0"]]}}
  :deploy-repositories [["releases" :clojars]]
  :repositories {"cloudera" "https://repository.cloudera.com/artifactory/cloudera-repos"
                 "jboss-third-party" "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
                 "unidata" "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases"}
  :javac-options ["-target" "1.6" "-source" "1.6" "-g"]
  :java-source-paths ["src"]
  :junit ["test"])

;; Cascalog uses insecure conjars repo
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))
