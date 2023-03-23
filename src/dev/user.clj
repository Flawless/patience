(ns user
  {:doc "Dev command center",
   :clj-kondo/config
   '{:linters
     {:unused-referred-var {:level :off},
      :unused-namespace {:level :off}}}}
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojure.tools.namespace.repl :as repl]
   [integrant.core :as ig]
   [patience.core :as patience]
   [patience.model :as-alias m]))

(def full-config
  {:db/patients {:type :dummy
                 #_#_:config {:store (atom [])}}
   :ring/handler {:patients (ig/ref :db/patients)
                  :serve-static? true}
   :jetty/server {:ring-handler (ig/ref :ring/handler)
                  :host "0.0.0.0"
                  :port 9501}})

(alter-var-root #'patience/config (constantly full-config))

;; css
(comment
  (do
    (require '[shadow.cljs.devtools.server.fs-watch :as fs-watch]
             '[shadow.css.build :as cb])

    (alter-var-root #'patience/config
                    assoc ::css {})

    (defn generate-css [css-ref]
      (let [result
            (-> @css-ref
                (cb/generate '{:ui {:include [patience*]}})
                (cb/write-outputs-to (io/file "resources/public" "css")))]

        (prn :CSS-GENERATED)
        (doseq [mod (:outputs result)
                {:keys [warning-type] :as warning} (:warnings mod)]
          (prn [:CSS (name warning-type) (dissoc warning :warning-type)]))
        (println)))

    (defn start
      {:shadow/requires-server true}
      [{:keys [css-ref css-watch-ref]}]

      ;; first initialize my css
      (reset! css-ref
              (-> (cb/start)
                  (cb/index-path (io/file "src" "main") {})))

      ;; then build it once
      (generate-css css-ref)

      ;; then setup the watcher that rebuilds everything on change
      (reset! css-watch-ref
              (fs-watch/start
               {}
               [(io/file "src" "main")
                (io/file "src" "dev")]
               ["cljs" "cljc" "clj"]
               (fn [updates]
                 (try
                   (doseq [{:keys [file event]} updates
                           :when (not= event :del)]
                     ;; re-index all added or modified files
                     (swap! css-ref cb/index-file file))

                   (generate-css css-ref)
                   (catch Exception e
                     (prn :css-build-failure)
                     (prn e))))))

      ::started)

    (defn stop [{:keys [css-ref css-watch-ref]}]
      (when-some [css-watch @css-watch-ref]
        (fs-watch/stop css-watch)
        (reset! css-ref nil))

      ::stopped)

    (defmethod ig/init-key ::css [_ _]
      (let [css {:css-ref (atom nil)
                 :css-watch-ref (atom nil)}
            _ (start css)]
        css))

    (defmethod ig/halt-key! ::css [_ css]
      (stop css))))

(comment
  ;; load system config
  (alter-var-root #'patience/config (constantly (patience/load-config "/etc/patience.edn")))
  (log/info "abc")

  patience/config

  (patience/start)

  (patience/stop)

  (alter-var-root #'patience/config dissoc ::css)

  (patience/restart)

  (repl/clear)
  (repl/refresh-all)

  (some? patience/system)

  patience/system

  ;; load sample patients (only on local repl)
  (require '[clojure.java.io :as io])
  (slurp (io/resource "patients-sample.edn"))

  (let [patients (:db/patients patience/system)]
    (doseq [patient sample-patients]
      (patience.db/create-patient! patients
                                   (update patient :date-of-birth clojure.instant/read-instant-date))))

  (def test-patients [{::m/id 1
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
                       ::m/insurance-number "11111005"}])

  ;; sometimes system may be lost and you have no other way to free port but kill repl, but if you are as lazy as I'm
  ;; you can use followed command to launch server on other port ¯\_(ツ)_/¯
  (alter-var-root #'patience/config assoc-in [:jetty/server :port] (+ 5000 (rand-int 1000)))

  (alter-var-root #'patience/config assoc-in [:db/patients :config :store])

  (alter-var-root #'patience/config assoc :db/patients {:type :sql
                                                        :config {:ds {:dbtype "postgres"
                                                                      :dbname "postgres"
                                                                      :user "postgres"
                                                                      :password "postgres"
                                                                      :host "localhost"}
                                                                 :trace? true}})

  (reset! (-> patience/system :db/patients :store) test-patients)
  (-> patience/system :db/patients )

  (patience/restart)

  (repl/clear)
  (repl/refresh)

  (some? patience/system)

  (def ds (jdbc/get-datasource {:dbtype "postgres"
                                :dbname "postgres"
                                :user "postgres"
                                :password "postgres"
                                :host "localhost"}))

 (require '[honey.sql :as sql]
          '[next.jdbc :as jdbc])

 (->> (jdbc/execute! ds ["SELECT * FROM pg_catalog.pg_tables"])
       (mapv :pg_tables/tablename)
       (some #{"patients"}))

  (jdbc/execute! ds (sql/format {:select [:*]
                                 :from [:patients]}))

  (jdbc/execute! ds (sql/format {:select [:*]
                                 :from [:patients]
                                 :where [:= :id 2]}))

  (jdbc/execute! ds (sql/format {:delete-from [:patients]
                                 :where [:= :id 1]}))

  (jdbc/execute! ds (sql/format {:insert-into [:patients]
                                 :columns [:patients/name]
                                 :values [["New Patient 2"]]
                                 :returning [:*]}))

  (jdbc/execute! ds (sql/format {:update :patients
                                 :where [:= :id 2]
                                 :set {:name "Patient"}
                                 :returning [:*]}))

  (jdbc/execute! ds (sql/format {:create-table [:patients :if-not-exists]
                                 :with-columns
                                 [[:id :serial :primary-key]
                                  [:name [:varchar 32] [:not nil]]
                                  [:gender [:varchar 32]]
                                  [:date-of-birth :date]
                                  [:address [:varchar 128]]
                                  [:insurance-number :int]]}))

  (jdbc/execute! ds (sql/format {:drop-table :patients})))
