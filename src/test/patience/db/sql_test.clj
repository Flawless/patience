(ns patience.db.sql-test
  (:require
   [clojure.test :refer [deftest is]]
   [patience.db.sql :as subject]
   [patience.model :as-alias m]))

(deftest drop-nils-test
  (is (= {:a 1 :c 2}
         (#'subject/drop-nils {:a 1 :b nil :c 2}))))

(deftest sql->api-test
  (is (= {::m/id 1
          ::m/name "Ivan"
          ::m/date-of-birth #inst "2022-01-01"}
       (#'subject/sql->api {:patients/id 1
                            :patients/name "Ivan"
                            :patients/date_of_birth #inst "2022-01-01"}))))

(deftest sql->api-test
  (is (= {:patients/id 1
          :patients/name "Ivan"
          :patients/date-of-birth #inst "2022-01-01"}
         (#'subject/api->sql {::m/id 1
                              ::m/name "Ivan"
                              ::m/date-of-birth #inst "2022-01-01"}))))
