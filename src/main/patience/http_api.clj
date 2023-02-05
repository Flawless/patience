(ns patience.http-api
  (:require
   [patience.db :as db]
   [ring.util.http-response :as http]))

(defn pong [_] {:status 200 :body "ok"})

(defn list-patients [patients _]
  (http/ok (db/list-patients patients)))

(defn get-patient [patients {{{:keys [id]} :path} :parameters}]
  (if-some [patient (db/get-patient patients id)]
    (http/ok patient)
    (http/not-found)))

(defn create-patient! [patients {{patient :body} :parameters}]
  (if-let [patient (db/create-patient! patients patient)]
    (http/ok patient)
    (http/bad-request)))

(defn delete-patient! [patients {{{:keys [id]} :path} :parameters}]
  (if (db/delete-patient! patients id)
    (http/ok {}) ; have to return something because otherwise http-fx doesn't consider request was successful
    (http/bad-request)))

(defn update-patient! [patients {{{:keys [id]} :path patient :body} :parameters}]
  (if-let [patient (db/update-patient! patients id patient)]
    (http/ok patient)
    (http/bad-request)))
