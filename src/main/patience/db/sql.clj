(ns patience.db.sql
  (:require
   next.jdbc.date-time
   [clojure.instant :as inst]
   [clojure.string :as s]
   [clojure.tools.logging :as log]
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [patience.db.protocol :as db.protocol]))

(defn- sql->api
  "Transform patient map from db to api form replacing key namespaces inside map with patience.model. Also replace
  underscores with dashes inside keywords."
  [patient]
  (reduce-kv (fn [acc k v]
               (assoc acc (keyword "patience.model" (s/replace (name k) #"_" "-")) v)) {} patient))

(defn- api->sql
  "Transform patient map from api form to sql replacing key namespaces inside map with name of patients table."
  [patient]
  (reduce-kv (fn [acc k v] (assoc acc (keyword "patients" (name k)) v)) {} patient))

(defn- normalize-filter
  "Make full filter for name/like when passed in simple form (just sting), otherwise return as is."
  [filter]
  (if (vector? filter)
    filter
    [:name :like filter]))

(defn- filter-errors
  "Challange filter over unsupported keys or predicates. Returns list of all errors when found."
  [[k p]]
  (let [key->preds {:name #{:eq :like :gt :lt}
                    :id #{:eq :gt :lt}
                    :gender #{:eq :like :gt :lt}
                    :date-of-birth #{:eq :gt :lt}
                    :address #{:eq :like :gt :lt}
                    :insurance-number #{:eq :gt :lt}}
        supported-pred? (key->preds k)]
    (cond
      (nil? supported-pred?) [{:key k
                               :error :unsupported-key}]
      (not (supported-pred? p)) [{:key k
                                  :pred p
                                  :error :unsupported-pred}])))

(defn- coerce-filter
  "Coerce filter depend on keys."
  [[k p v]]
  (let [v' (case k
             :date-of-birth (inst/read-instant-date v)
             :insurance-number (parse-long v)
             v)]
    [k p v']))

(def schema
  (sql/format {:create-table [:patients :if-not-exists]
               :with-columns
               [[:id :serial]
                [:name [:varchar 32] [:not nil]]
                [:gender [:varchar 32]]
                [:date-of-birth :date]
                [:address [:varchar 128]]
                [:insurance-number :int]]}))

(defn- get-patients
  ([] {:select [:*] :from [:patients]})
  ([filters]
   (reduce (fn [acc filter]
             (let [[k p v] (-> filter
                               normalize-filter
                               coerce-filter)
                   where-clause (case p
                                  :eq [:= k v]
                                  :lt [:< k v]
                                  :gt [:> k v]
                                  :like [:like k (str \% v \%)])]
               (update acc :where conj where-clause)))
           {:select [:*] :from [:patients]}
           filters)))

(defn- get-patient [sought-id]
  {:select [:*]
   :from [:patients]
   :where [:= :id sought-id]})

(defn- create-patient [patient]
  (let [{:keys [columns values]}
        (reduce-kv (fn [acc k v]
                     (-> acc
                         (update :columns conj k)
                         (update :values conj v)))
                   {}
                   patient)]
    {:insert-into [:patients]
     :columns columns
     :values [values]
     :returning [:*]}))

(defn- delete-patient [id]
  {:delete-from [:patients]
   :where [:= :id id]})

(defn- update-patient [id patient]
  {:update :patients
   :where [:= :id id]
   :set patient})

(defn- drop-nils [m]
  (reduce-kv (fn [acc k v]
               (cond-> acc
                 v (assoc k v)))
             {} m))

(defn- create-patient! [ds patient]
  (->> (api->sql patient)
           (create-patient)
           (sql/format)
           (jdbc/execute! ds)
           (first)
           (drop-nils)
           (sql->api)))

(defn- get-patient-by-id [ds patient-id]
  (->> (get-patient patient-id)
       (sql/format)
       (jdbc/execute! ds)
       (first)
       (drop-nils)
       (sql->api)))

(defn- list-all-patients [ds]
  (->> (get-patients)
       (sql/format)
       (jdbc/execute! ds)
       (map drop-nils)
       (map sql->api)))

(defn- list-filtered-patients [ds filters]
  (->> filters
       (map normalize-filter)
       (get-patients)
       (sql/format)
       (jdbc/execute! ds)
       (map drop-nils)
       (map sql->api)))

(defn- delete-patient! [ds patient-id]
  (->> (delete-patient patient-id)
       (sql/format)
       (jdbc/execute! ds)))

(defn- update-patient! [ds patient-id new-state]
  (->> (api->sql new-state)
       (update-patient patient-id)
       (sql/format)
       (jdbc/execute! ds)
       (first)
       (drop-nils)
       (sql->api)))

(defrecord Patients [ds]
    db.protocol/IPatients
    (list-filtered [_this filters]
      (list-filtered-patients ds filters))

    (list-all [_this]
      (list-all-patients ds))

    (get-by-id [_this patient-id]
      (get-patient-by-id ds patient-id))

    (create! [_this patient]
      (create-patient! ds patient))

    (delete! [_this patient-id]
      (delete-patient! ds patient-id))

    (update! [_this patient-id new-state]
      (update-patient! ds patient-id new-state)))

(defn patients
  [{:keys [trace? ds] :as _config}]
  (let [_ (log/info "Connecting to sql db...")
        ds (cond-> (jdbc/get-datasource ds)
             trace? (jdbc/with-logging
                      (fn [sym sql-params]
                        (log/info sym sql-params)
                        (System/currentTimeMillis))
                      (fn [sym state result]
                        (log/info sym
                                  (- (System/currentTimeMillis) state)
                                  result))))
        _ (log/info "Connection to db successfully established.")
        _ (log/info "Ensuring postgres schema...")
        _ (jdbc/execute! ds schema)
        _ (log/info "Schema applied.")]
    (->Patients ds)))
