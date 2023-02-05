(ns patience.ui.db
  (:require
   [clojure.string :as s]
   [patience.model :as-alias m]
   [shadow.grove :as sg]
   [shadow.grove.db :as db]
   [shadow.grove.eql-query :as eql]
   [shadow.grove.events :as ev]))

(defn init!
  {::ev/handle ::m/init!}
  [env _]
  (sg/queue-fx env :http-api
               {:request {:uri "/patients"}
                :on-success {:e ::init-data}}))

(defn save-patient!
  {::ev/handle ::m/save-patient!}
  [env {{::m/keys [id] :as patient} :patient}]
  (sg/queue-fx env :http-api
               {:request {:uri (str "/patients/" id)
                          :method :put
                          :body patient}
                :on-success {:e ::m/on-success-update-patient!}}))

(defn delete-patient!
  {::ev/handle ::m/delete-patient!}
  [env {ident :patient-ident}]
  (sg/queue-fx env :http-api
               {:request {:uri (str "/patients/" (get-in env [:db ident ::m/id]))
                          :method :delete}
                :on-success {:e ::m/on-success-delete-patient!
                             :patient-ident ident}}))

(defn create-patient!
  {::ev/handle ::m/create-patient!}
  [env]
  (sg/queue-fx env :http-api
               {:request {:uri (str "/patients")
                          :method :post
                          :body {::m/name "New Patient"}}
                :on-success {:e ::m/on-success-create-patient!}}))

(defn on-success-create-patient!
  {::ev/handle ::m/on-success-create-patient!}
  [env {:keys [result]}]
  (update env :db #(-> %
                       (db/add ::m/patient result))))

(defn on-success-update-patient!
  {::ev/handle ::m/on-success-update-patient!}
  [env {:keys [result patient-ident]}]
  (update env :db #(db/update-entity % ::m/patient patient-ident (constantly result))))

(defn on-success-delete-patient!
  {::ev/handle ::m/on-success-delete-patient!}
  [env {:keys [patient-ident]}]
  (update env :db #(-> %
                       (dissoc patient-ident)
                       (assoc ::m/current-patient nil))))

(defn init-data
  {::ev/handle ::init-data}
  [env {:keys [result]}]
  (tap> [:result result])
  (update env :db #(-> %
                       (assoc ::m/init-complete? true)
                       (cond-> result (db/merge-seq ::m/patient result [::m/patients])))))

(defmethod eql/attr ::m/patients-count [env db _ _]
  (->> (db/all-of db ::m/patient)
       (mapv :db/ident)
       (count)))

(defmethod eql/attr ::m/filtered-patients [env db _ _]
  ;; [_ {::m/keys [current-filter] :as db} _ _ _]
  (let [#_#_filter-fn (fn [{:patient/keys [name insurance-number]}]
                    (or (string/includes? name current-filter)
                        (string/includes? (str insurance-number) current-filter)))]
    (->> (db/all-of db ::m/patient)
         ;; (filter filter-fn)
         (mapv :db/ident))))

(defmethod eql/attr ::m/patient [env db patient _]
  ;; [_ {::m/keys [current-filter] :as db} _ _ _]
  (get patient db))

(defn start-edit-patient!
  {::ev/handle ::m/start-edit-patient!}
  [env {:keys [patient-ident]}]
  (assoc-in env [:db patient-ident ::m/editing?] true))

(defn- cancel-edit-patient
  [env patient]
  (cond-> env
    patient (update-in [:db patient] dissoc ::m/editing?)))

(defn cancel-edit-patient!
  {::ev/handle ::m/cancel-edit-patient!}
  [env {:keys [patient-ident]}]
  (cancel-edit-patient env patient-ident))

(defn select-patient!
  {::ev/handle ::m/select-patient!}
  [{{::m/keys [current-patient]} :db :as env} {:keys [patient-ident]}]
  (-> env
      (assoc-in [:db ::m/current-patient] patient-ident)
      (cancel-edit-patient current-patient)))

(defn request-error!
  {::ev/handle ::m/request-error!}
  [env e]
  (tap> e)
  (assoc-in env [:db :error] e))

(defn ui-route!
  {::ev/handle :ui/route!}
  [{:keys [db] :as env} {:keys [tokens] :as msg}]

  (tap> [:route msg])
  (let [[main & more] tokens]
    (case main
      "grove/patients"
      (update env :db assoc
              ::m/current-page {:id :patient})

      "grove/patient"
      (let [[patient-id sub-page] more
            patient-ident (db/make-ident ::m/patient patient-id)]
        (update env :db
                (fn [db]
                  (-> db
                      (assoc ::m/current-page
                             {:id patient-id
                              :ident patient-ident})
                      (assoc ::m/current-patient patient-ident)))))

      (do (js/console.warn "unknown-route" msg)
          (ev/queue-fx env :ui/redirect! {:token "/grove/patients"})))))
