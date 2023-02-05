(ns patience.db
  (:require
   [patience.db.dummy :as db.dummy]
   [patience.db.protocol :as db.protocol]
   [patience.model :as-alias m]
   [patience.db.sql :as db.sql]))

(defn patients
  ([type] (patients type nil))
  ([type config]
   (case type
     :sql (db.sql/patients config)
     :dummy (db.dummy/patients config))))

(defn get-patient
  [patients id]
  (db.protocol/get-by-id patients id))

(defn list-patients
  ([patients] (db.protocol/list-all patients))
  ([patients filters]
   (if (seq filters)
     (db.protocol/list-filtered patients filters)
     (db.protocol/list-all patients))))

(defn create-patient!
  [patients patient]
  (db.protocol/create! patients patient))

(defn delete-patient!
  [patients id]
  (db.protocol/delete! patients id))

(defn update-patient!
  [patients id new-state]
  {:pre [(= (::m/id new-state) id)]}
  (db.protocol/update! patients id new-state))
