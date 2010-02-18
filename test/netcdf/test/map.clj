(ns netcdf.test.map
  (:use clojure.test netcdf.map netcdf.location))

(deftest test-options->map
  (is (= (options->params {:center "0,0"}) "center=0,0"))
  (is (= (options->params {:center "0,0" :zoom 1}) "center=0,0&zoom=1")))

(deftest test-parse-options
  (is (= (parse-options {:center (make-location 0 0)})
         {:size "300x200", :maptype "roadmap", :sensor false, :zoom 1})))
  
(deftest test-static-map-url
  (is (= (static-map-url (make-location 0 0))
         "http://maps.google.com/maps/api/staticmap?size=300x200&maptype=roadmap&sensor=false&zoom=1")))

(deftest test-static-map-image
  (is (static-map-image (make-location 0 0))))
