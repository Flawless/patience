(ns patience.ui.env
  (:require
   [patience.model :as-alias m]
   [shadow.grove :as sg]
   [shadow.grove.db :as db]))


(def schema
  {::m/patient
   {:type :entity
    :primary-key ::m/id
    :attrs {}
    :joins {}}})

(defonce data-ref
  (-> {::m/id-seq 0
       ::m/editing nil
       ::m/current-patient nil}
      (db/configure schema)
      (atom)))

(defonce rt-ref
  (-> {}
      (sg/prepare data-ref ::db)))
