(ns patience.db.dummy-test
  (:require
   [clojure.test :refer [deftest is]]
   [patience.db.dummy :as subject]
   [patience.model :as-alias m]))

(deftest get-patient-test
  (let [patient-1 {::m/id 1
                   ::m/name "Ivan Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "1970-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111106"}
        patient-2 {::m/id 2
                   ::m/name "Ivan Ivanovich Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "2070-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111005"}
        patient-id 2]
    (is (= patient-2 (#'subject/get-patient [patient-1 patient-2] patient-id)))))

(deftest list-patient-test
  (let [patient-1 {::m/id 1
                   ::m/name "Ivan Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "1970-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111106"}
        patient-2 {::m/id 2
                   ::m/name "Fedor Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "2070-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111005"}]
    (is (= [patient-2] (#'subject/get-patients [patient-1 patient-2] [[:name :like "Fedor"]])))
    (is (= [patient-2] (#'subject/get-patients [patient-1 patient-2] [[:date-of-birth :gt "2000-01-01"]])))))

(deftest create-patient-test
  (let [patient-1 {::m/id 1
                   ::m/name "Ivan Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "1970-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111106"}
        patient-2 {::m/id 2
                   ::m/name "Ivan Ivanovich Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "2070-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111005"}]
    (is (= #{patient-1 patient-2} (set (#'subject/create-patient [patient-1] patient-2))))))

(deftest delete-patient-test
  (let [patient-1 {::m/id 1
                   ::m/name "Ivan Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "1970-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111106"}
        patient-2 {::m/id 2
                   ::m/name "Ivan Ivanovich Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "2070-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111005"}
        patient-id 1]
    (is (= [patient-2] (#'subject/delete-patient [patient-1 patient-2] patient-id)))))

(deftest update-patient-test
  (let [patient-1 {::m/id 1
                   ::m/name "Ivan Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "1970-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111106"}
        patient-2 {::m/id 2
                   ::m/name "Ivan Ivanovich Ivanov"
                   ::m/gender "male"
                   ::m/date-of-birth "2070-01-01"
                   ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                   ::m/insurance-number "11111005"}
        patient-2' {::m/id 2
                    ::m/name "Ivan Ivanovich Petrov"
                    ::m/gender "male"
                    ::m/date-of-birth "2070-01-01"
                    ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
                    ::m/insurance-number "11111005"}
        patient-id 2]
    (is (= #{patient-1 patient-2'}
           (set (#'subject/update-patient [patient-1 patient-2] patient-id patient-2'))))))

(deftest prepare-new-patient-test
  (let [patient {::m/name "New Patient"}
        db [{::m/id 1
             ::m/name "Ivan Ivanov"
             ::m/gender "male"
             ::m/date-of-birth "1970-01-01"
             ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
             ::m/insurance-number "11111106"}
            {::m/id 2
             ::m/name "Ivan Ivanovich Ivanov"
             ::m/gender "male"
             ::m/date-of-birth "2070-01-01"
             ::m/address "23, Ulitsa Ilyinka, 103132, Moscow, Russia"
             ::m/insurance-number "11111005"}]]
    (is (= (assoc patient ::m/id 3) (#'subject/prepare-new-patient db patient)))))
