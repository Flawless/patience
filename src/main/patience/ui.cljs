(ns patience.ui
  {:dev/always true}
  (:require
   [shadow.grove :as sg]
   [shadow.grove.events :as ev]
   [shadow.grove.http-fx :as http-fx]
   [shadow.grove.history :as history]
   [shadow.grove.transit :as transit]
   [patience.ui.env :as env]
   [patience.ui.views :as views]
   [patience.ui.db]
   [patience.model :as-alias m]))

(defonce root-el
  (js/document.getElementById "app"))

(defn render []
  (sg/render env/rt-ref root-el
    (views/ui-root)))

(defn register-events! []
  (ev/register-events! env/rt-ref))

(defn init []
  (js/console.log "initialize")
  (register-events!)
  ;; useful for debugging until there are actual tools for this
  (when ^boolean js/goog.DEBUG
    (swap! env/rt-ref assoc :shadow.grove.runtime/tx-reporter
           (fn [{:keys [event] :as report}]
             ;; alternatively use tap> and the shadow-cljs UI
             (tap> [:evt (:e event) event report]))))

  (transit/init! env/rt-ref)
  (history/init! env/rt-ref
                 {:use-fragment true
                  :start-token "/all"})

  (sg/reg-fx env/rt-ref :http-api
             (http-fx/make-handler
              {:on-error {:e ::m/request-error!}
               :base-url "/api"
               :request-format :transit}))

  (sg/run-tx! env/rt-ref {:e ::m/init!})

  (render))

(defn ^:dev/after-load reload! []
  (js/console.log "reloading")
  (register-events!)
  (render))
