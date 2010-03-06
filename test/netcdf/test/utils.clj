(ns netcdf.test.utils
  (:use clojure.test netcdf.utils))

(deftest test-with-meta+
  (let [obj [1 2] m {:key "val"}]
    (is (= (with-meta+ obj {}) obj))
    (is (= (with-meta+ obj m) obj))
    (is (= (meta (with-meta+ obj m)) m))
    (is (= (meta (with-meta+ (with-meta obj {:key "x"}) m)) m))
    (is (= (meta (with-meta+ (with-meta obj m) {:key2 "val2"})) (merge m {:key2 "val2"})))))


