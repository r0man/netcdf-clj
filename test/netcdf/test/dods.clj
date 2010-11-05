(ns netcdf.test.dods
  (:import java.util.Calendar java.io.File java.net.URI)
  (:use [clj-time.core :only (date-time year month day hour)]
        clj-time.format
        netcdf.dods
        netcdf.repository
        netcdf.test.helper
        clojure.test))

(def *repository* (lookup-repository "akw"))

(def *dods* "file:///home/roman/workspace/netcdf-clj/test/fixtures/dods.xml")

(deftest test-inventory-url
  (is (= (inventory-url *repository*) (str (:url *repository*) "/xml"))))

(deftest test-latest-reference-time
  (is (= (latest-reference-time *repository*)
         (last (reference-times *repository*)))))

(deftest test-make-repository
  (let [repository (make-repository "akw" "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw" "Alaskan Waters")]
    (is (= (:name repository ) "akw"))
    (is (= (:description repository ) "Alaskan Waters"))
    (is (= (:url repository ) "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw"))))

(deftest test-parse-inventory
  (let [datasets (parse-inventory "test-resources/dods.xml")]
    (is (= 4 (count datasets)))
    (let [dataset (first datasets)]
      (is (= "/fnl/fnl20101030/fnlflx_00z" (:name dataset)))
      (is (= "gdas1 flux file at 00Z30oct2010, downloaded Oct 30 06:55 UTC" (:description dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/fnl/fnl20101030/fnlflx_00z.das" (:das dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/fnl/fnl20101030/fnlflx_00z.dds" (:dds dataset)))
      (is (= "http://nomads.ncep.noaa.gov:9090/dods/fnl/fnl20101030/fnlflx_00z" (:dods dataset))))))

(deftest test-parse-dods
  (let [dods (parse-dods *dods*)]
    (is (= (first dods) "http://nomad5.ncep.noaa.gov:9090/dods/aofs/ofs"))
    (is (= (last dods) "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3/nww320091124/nww3_12z"))))

(deftest test-parse-reference-times
  (let [times (parse-reference-times *dods* "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3")]
    (is (= (first times) (date-time 2009 9 7 0 0 0)))
    (is (= (last times) (date-time 2009 11 24 12 0 0)))))

(deftest test-dataset-directory
  (is (= (dataset-directory *repository* *valid-time*)
         (str (:name *repository*) (unparse (formatters :basic-date) *valid-time*)))))

(deftest test-dataset-filename
  (is (= (dataset-filename *repository* *valid-time*)
         (str (:name *repository*) "_" (unparse (formatters :hour) *valid-time*) "z"))))

(deftest test-dataset-url
  (is (= (dataset-url *repository* *valid-time*)
         (str "http://nomad5.ncep.noaa.gov:9090/dods/waves/"
              (:name *repository*) "/"
              (dataset-directory *repository* *valid-time*) "/"
              (dataset-filename *repository* *valid-time*)))))

(deftest test-dataset-url->time
  (let [url "http://nomad5.ncep.noaa.gov:9090/dods/waves/nww3/nww320090907/nww3_00z"]
    (let [time (dataset-url->time url)]
      (is (= time (date-time 2009 9 7 0 0 0))))
    (let [time (dataset-url->time (URI. url))]
      (is (= time (date-time 2009 9 7 0 0 0))))))

(deftest test-reference-times
  (let [reference-times (reference-times *repository*)]
    (is (not (empty? reference-times)))))

(deftest test-latest-reference-time
  (is (not (nil? (latest-reference-time *repository*)))))

(deftest test-valid-time->reference-time
  (are [valid-time reference-time]
    (is (= (valid-time->reference-time valid-time) reference-time))
    (date-time 2010 3 25 0 0) (date-time 2010 3 25 0 0)
    (date-time 2010 3 25 5 59) (date-time 2010 3 25 0 0)
    (date-time 2010 3 25 6 0) (date-time 2010 3 25 6 0)
    (date-time 2010 3 25 11 59) (date-time 2010 3 25 6 0)
    (date-time 2010 3 25 12 0) (date-time 2010 3 25 12 0)))

(deftest test-local-url  
  (let [uri (local-url *repository* *valid-time*)]
    (is (isa? (class uri) java.net.URI))
    (is (= (str uri) (str "file:"  *local-url* File/separator (:name *repository*) File/separator
                          (unparse (formatters :basic-date) *valid-time*) File/separator "t" (unparse (formatters :hour) *valid-time*) "z.nc"))))
  (let [uri (local-url *repository* *valid-time* *variable*)]
    (is (isa? (class uri) java.net.URI))
    (is (= (str uri) (str "file:" *local-url* File/separator (:name *repository*) File/separator
                          *variable* File/separator (unparse (formatters :basic-date) *valid-time*) File/separator "t" (unparse (formatters :hour) *valid-time*) "z.nc")))))

(deftest test-download-variable
  (let [uri (download-variable *repository* *variable* *valid-time*)]
    (is (.exists (File. uri)))))

