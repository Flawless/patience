(ns patience.main
  (:gen-class)
  (:require
   [clojure.tools.logging :as log]
   [patience.core :as patience]))

(defn -main [& _args]
  (if-let [config (patience/load-config "/etc/patience.edn")]
    (alter-var-root #'patience/config (constantly config))
    (log/warn "Using default config."))
  (patience/start))

(comment
  (-main))
