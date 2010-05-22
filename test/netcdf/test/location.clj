(ns netcdf.test.location
  (:use clojure.test netcdf.location))

(def *berlin* (make-location 52.523 13.411))
(def *paris* (make-location 48.857 2.351))
(def *vienna* (make-location 48.209 16.373))

(deftest test-make-location
  (let [location (make-location 52.523 13.411)]
    (is (= (latitude location) 52.523))
    (is (= (longitude location) 13.411)))
  (let [location (make-location {:latitude 52.523 :longitude 13.411})]
    (is (= (latitude location) 52.523))
    (is (= (longitude location) 13.411)))  
  (let [location (make-location "52.523" "13.411")]
    (is (= (latitude location) 52.523))
    (is (= (longitude location) 13.411))))

(deftest test-location?
  (is (location? (make-location 1 2)))
  (is (not (location? nil))))

(deftest test-parse-location
  (let [location (parse-location "52.52 13.41")]
    (is (= (latitude location) 52.52))
    (is (= (longitude location) 13.41)))
  (let [location (parse-location "52.52,13.41")]
    (is (= (latitude location) 52.52))
    (is (= (longitude location) 13.41)))
  (let [location (parse-location "52.52;13.41")]
    (is (= (latitude location) 52.52))
    (is (= (longitude location) 13.41)))
  (let [location (parse-location "52.52\t13.41")]
    (is (= (latitude location) 52.52))
    (is (= (longitude location) 13.41))))

(deftest test-destination-point
  (let [location (destination-point *berlin* 30 100)]
    (is (= (latitude location) 53.298866294161215))
    (is (= (longitude location) 14.16092284183496))))

(deftest test-distance
  (is (= (distance *berlin* *berlin*) 0.0))
  (is (= (distance *berlin* *paris*) 880.2565917803378)))

(deftest test-north?
  (is (north? *berlin* *paris*))
  (is (north? *paris* *vienna*))
  (is (not (north? *berlin* *berlin*)))
  (is (not (north? (make-location (dec (latitude *berlin*)) (longitude *berlin*)) *berlin*)))
  (is (north? (make-location (inc (latitude *berlin*)) (longitude *berlin*)) *berlin*)))

(deftest test-east?
  (is (east? *berlin* *paris*))
  (is (east? *vienna* *berlin*))
  (is (not (east? *berlin* *berlin*)))
  (is (not (east? (make-location (latitude *berlin*) (dec (longitude *berlin*))) *berlin*)))
  (is (east? (make-location (latitude *berlin*) (inc (longitude *berlin*))) *berlin*)))

(deftest test-south?
  (is (south? *paris* *berlin*))
  (is (south? *vienna* *berlin*))
  (is (not (south? *berlin* *berlin*)))
  (is (not (south? (make-location (inc (latitude *berlin*)) (longitude *berlin*)) *berlin*)))
  (is (south? (make-location (dec (latitude *berlin*)) (longitude *berlin*)) *berlin*)))

(deftest test-west?
  (is (west? *paris* *berlin*))
  (is (west? *paris* *vienna* ))
  (is (not (west? *berlin* *berlin*)))
  (is (not (west? (make-location (latitude *berlin*) (inc (longitude *berlin*))) *berlin*)))
  (is (west? (make-location (latitude *berlin*) (dec (longitude *berlin*))) *berlin*)))

(deftest test-north-east?
  (is (north-east? *berlin* *paris*))
  (is (not (north-east? *paris* *berlin*)))
  (is (not (north-east? *berlin* *berlin*))))

(deftest test-south-east?
  (is (south-east? *vienna* *berlin*))
  (is (not (south-east? *berlin* *vienna*)))
  (is (not (south-east? *berlin* *berlin*))))

(deftest test-south-west?
  (is (south-west? *paris* *berlin*))
  (is (not (south-west? *berlin* *paris*)))
  (is (not (south-west? *vienna* *berlin*))))

(deftest test-north-west?
  (is (north-west? *paris* *vienna*))
  (is (not (north-west? *vienna* *paris*)))
  (is (not (north-west? *paris* *paris*))))

(deftest test-latitude-distance
  (is (= (latitude-distance *berlin* *berlin*) 0))
  (is (= (latitude-distance *berlin* *paris*) -3.666000000000004))
  (is (= (latitude-distance *paris* *berlin*) 3.666000000000004)))

(deftest test-longitude-distance
  (is (= (longitude-distance *berlin* *berlin*) 0))
  (is (= (longitude-distance *berlin* *paris*) -11.059999999999999))
  (is (= (longitude-distance *paris* *berlin* ) 11.059999999999999)))

(deftest test-latitude-range
  (is (empty? (latitude-range 0 0)))
  (is (= (latitude-range 0 1) [0]))
  (is (= (latitude-range 0 1 0.5) [0]))
  (let [range (latitude-range 0 90)]
    (is (= (count range) 90))
    (is (= (first range) -89))
    (is (= (last range) 0))))

(deftest test-longitude-range
  (is (empty? (longitude-range 0 0)))
  (is (= (longitude-range 0 1) [0]))
  (is (= (longitude-range 0 1 0.5) [0]))
  (let [range (longitude-range 0 180)]
    (is (= (count range) 180))
    (is (= (first range) 0))
    (is (= (last range) 179))))

(deftest test-location-range
  (is (empty? (location-range (make-location 0 0) (make-location 0 0))))
  (is (= (location-range (make-location 0 0) (make-location 1 1))
         [(make-location 0 0)]))
  (is (= (location-range (make-location 0 0) (make-location 1 1) :lat-step 0.5)
         [(make-location 0 0)]))
  (is (= (location-range (make-location 0 0) (make-location 1 1) :lon-step 0.5)
         [(make-location 0 0)]))
  (is (= (location-range (make-location 0 0) (make-location 2 1) :lon-step 0.5)
         [(make-location -1 0) (make-location 0 0)]))
  (let [range (location-range (make-location 0 0) (make-location 2 2))]
    (is (= (count range) 4))
    (is (= (first range) (make-location -1 0)))
    (is (= (nth range 1) (make-location -1 1)))
    (is (= (nth range 2) (make-location 0 0)))
    (is (= (last range) (make-location 0 1)))))

(deftest test-location->array
  (is (= (location->array (make-location 78 0)) [78 0])))

(deftest test-location-rect
  (is (empty? (location-rect (make-location 0 0) :width 0)))
  (is (empty? (location-rect (make-location 0 0) :height 0)))
  (is (= (location-rect (make-location 0 0)) [(make-location 0 0)]))
  (is (= (location-rect (make-location 0 0) :width 2)
         [(make-location 0 0) (make-location 0 1) (make-location -1 0) (make-location -1 1)]))
  (is (= (location-rect (make-location 0 0) :width 2 :height 1)
         [(make-location 0 0) (make-location 0 1)])))

(deftest test-location->map
  (is (= (location->map (make-location 1 2)) {:latitude 1 :longitude 2})))


(deftest test-parse-dms
  (is (= (parse-dms "51° 28' 40.12\" N") 51.57811111111111))
  (is (= (parse-dms "000° 00' 0.531\" W") -0.0014750000000000002)))
