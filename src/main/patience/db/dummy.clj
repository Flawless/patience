(ns patience.db.dummy
  (:require
   [clojure.string :as s]
   [patience.db.protocol :as db.protocol]
   [patience.model :as-alias m]))

(defn- gt
  "Compare two objects, return `true` when `x` is logically 'greater than' y"
  [x y]
  (> 0 (compare x y)))

(defn- lt
  "Compare two objects, return `true` when `x` is logically 'less than' y"
  [x y]
  (gt y x))

(defn- get-patients
  "Return patients in db, apply filters when provided. All filters considered with logical AND so result list satisfies
  all filters."
  ([db] db)
  ([db filters]
   (if (seq filters)
     (let [f (->> filters
                  (map (fn [filter] (if (vector? filter) filter [:name :like filter])))
                  (map (fn [[field pred value]]
                         (let [k (keyword "patience.model" (name field))]
                           (case pred
                             :eq (comp #{value} k)
                             :like (comp #(s/includes? % value) k)
                             :gt (comp (partial gt value) k)
                             :lt (comp (partial lt value) k)))))
                  (apply every-pred))]
       (filter f db))
     (get-patients db))))

(defn- get-patient
  "Return a patient with given `sought-id`"
  [db sought-id]
  (some (fn [{::m/keys [id] :as patient}]
                   (when (= id sought-id)
                     patient))
                 db))

(defn- prepare-new-patient
  "Associate new unique in `db` id into `patient` map."
  [db patient]
  (let [id (transduce (map ::m/id) max 1 db)]
    (assoc patient ::m/id (inc id))))

(defn- create-patient
  "Return db, adding given `patient` to `db`"
  [db patient]
  (conj db patient))

(defn- delete-patient
  "Return db without patient with given `id`"
  [db id]
  (remove #(-> % ::m/id #{id}) db))

(defn- update-patient
  "Return db replacing patient by `id` with given `patient` map"
  [db id patient]
  (-> (remove #(-> % ::m/id #{id}) db)
      (conj patient)))

(defrecord Patients [store]
    db.protocol/IPatients

    (amount [_this filters] (count (get-patients @store filters)))
    (list-by [_this {filters :filters}] (get-patients @store filters))
    (get-by-id [_this patient-id] (get-patient @store patient-id))
    (create! [_this patient]
      (let [patient' (prepare-new-patient @store patient)
            _ (swap! store create-patient patient')]
        patient'))
    (delete! [_this patient-id] (swap! store delete-patient patient-id))
    (update! [_this patient-id new-state]
      (swap! store update-patient patient-id new-state)
      new-state))

(defn patients [config]
  (let [defconfig {:store (atom [])}]
    (map->Patients (merge defconfig config))))
