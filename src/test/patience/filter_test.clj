(ns patience.filter-test
  (:require
   [clojure.test :refer [deftest is]]
   [patience.filter :as subject]))

(deftest parse-filters-test
  (is (= [[:name :eq "Ivan Ivanov"]
          [:date-of-birth :gt "2021-12-01"]
          "oh !@#$"]
         (subject/parse-filters ":name/eq \"Ivan Ivanov\" :date-of-birth/gt 2021-12-01 \"oh !@#$\"")))
  (is (= ["ABC"] (subject/parse-filters "ABC")))
  (is (= [[:name :like "hello"] "oh" "!@#$"] (subject/parse-filters ":name/like hello oh !@#$"))))
