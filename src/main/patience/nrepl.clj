(ns patience.nrepl
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [nrepl.server :as nrepl]))

(defmethod ig/init-key ::server
  [_ {:keys [port]}]
  (let [server (nrepl/start-server :port port)
        _      (log/info "nREPL Server started on port" port)]
    server))

(defmethod ig/halt-key! ::server
  [_ server]
  (nrepl/stop-server server))
