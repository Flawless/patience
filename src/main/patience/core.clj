(ns patience.core
  (:require
   [clojure.core :refer [alter-var-root]]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojure.tools.namespace.repl :as repl]
   [integrant.core :as ig]
   [patience.db :as db]
   [patience.handler :as handler]
   [patience.model :as-alias m]
   [ring.adapter.jetty]
   [patience.nrepl]))

(def full-config
  {:db/patients {:type :dummy
                 :config {:store (atom [])}}
   :ring/handler {:patients (ig/ref :db/patients)}
   :jetty/server {:ring-handler (ig/ref :ring/handler)
                  :host "0.0.0.0"
                  :port 9501}})

(defmethod ig/init-key :db/patients [_ {:keys [type config]}]
  (db/patients type config))


(defmethod ig/init-key :ring/handler [_ {:keys [patients serve-static?]
                                         :or {serve-static? true}}]
  (fn [req]
    ;; TODO move logging to somewhere else (probably middleware)
    (log/infof "<< recv incoming request  %s" req)
    (let [resp ((handler/app patients serve-static?) req)
          _ (log/infof ">> sent outgoing response %s" resp)]
      resp)))

(defmethod ig/init-key :jetty/server
  [_ {:keys [ring-handler host port]}]
  (let [server (ring.adapter.jetty/run-jetty ring-handler {:host host
                                                           :port port
                                                           :join? false})
        _      (log/info "Jetty Server started on host" host "and port" port)]
    server))

(defmethod ig/halt-key! :jetty/server
  [_ server]
  (.stop server))

(defonce system nil)

(def config full-config)

(defn- -start [system]
  (when system
    (ig/halt! system))
  (-> config ig/prep ig/init))

(defn- -stop [system]
  (when system
    (ig/halt! system))
  nil)

(defn load-config
  [f]
  (when (nil? f)
    (log/infof "No config file specified. Using default location."))
  (let [f (or f "/etc/patience.edn")]
    (if (.exists (io/file f))
      (let [_ (log/infof "Loading config from %s." f)
            cfg (some-> f slurp ig/read-string)
            _ (log/debugf "Loaded config: %s" cfg)]
        cfg)
      (log/warn "No config file supported."))))

(defn start
  "Perform side effects required to launch the system, construct an instance of it and bind it to `system`. "
  []
  (alter-var-root #'system -start)
  :started)

(defn stop
  "Stop the instance of system, binded into `system`, perform side effects to shut it down and release resources. Unbind
  `system`."
  []
  (alter-var-root #'system -stop)
  :stoped)

(defn restart
  "Stop the `system` if it's started and reloads changed namespaces."
  []
  (stop)
  (repl/refresh :after 'patience.core/start))
