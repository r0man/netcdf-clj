(ns netcdf.test.repository
  (:use clojure.test netcdf.repository))

(deftest test-repository?
  (is (not (repository? "")))
  (is (not (repository? nil)))
  (is (repository? (make-repository "test" "http://example.com" "Test Model"))))

(deftest test-make-repository
  (let [repository (make-repository "akw" "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw" "Regional Alaska Waters Wave Model")]
    (is (repository? repository))
    (is (= (:name repository) "akw"))
    (is (= (:description repository) "Regional Alaska Waters Wave Model"))
    (is (= (:url repository) "http://nomad5.ncep.noaa.gov:9090/dods/waves/akw"))))

(deftest test-lookup-repository
  (let [repository (make-repository "test" "http://example.com" "Test Model")]    
    (is (nil? (lookup-repository (:name repository))))
    (register-repository repository)
    (is (= (lookup-repository (:name repository)) repository))
    (unregister-repository repository)
    (is (every? #(repository? (lookup-repository %))
                ["akw" "enp" "nah" "nph" "nww3" "wna" "gfs-hd"]))))

(deftest test-register-repository
  (let [repository (make-repository "test" "http://example.com" "Test Model")]    
    (is (= (register-repository repository) repository))
    (is (= (lookup-repository (:name repository)) repository))
    (unregister-repository repository)))

(deftest test-unregister-repository
  (let [repository (make-repository "test" "http://example.com" "Test Model")]    
    (register-repository repository)
    (is (= (unregister-repository repository) repository))
    (is (nil? (lookup-repository (:name repository))))))
