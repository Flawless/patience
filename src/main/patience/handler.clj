(ns patience.handler
  (:require
   [clojure.string :as s]
   [muuntaja.core :as m]
   [patience.db :as db]
   [patience.http-api :as http-api]
   [patience.views :as views]
   [reitit.coercion.malli :as rcm]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.dev :as dev]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(defn- hidden-method
  [request]
  (some-> (or (get-in request [:form-params "_method"])         ;; look for "_method" field in :form-params
              (get-in request [:multipart-params "_method"]))   ;; or in :multipart-params
          s/lower-case
          keyword))

(def wrap-hidden-method
  {:name ::wrap-hidden-method
   :wrap (fn [handler]
           (fn [request]
             (if-let [fm (and (= :post (:request-method request)) ;; if this is a :post request
                              (hidden-method request))]           ;; and there is a "_method" field
               (handler (assoc request :request-method fm)) ;; replace :request-method
               (handler request))))})

(def ApiPatient
  [:map
   [:patience.model/name {:optional true} string?]
   [:patience.model/gender {:optional true} string?]
   [:patience.model/date-of-birth {:optional true} inst?]
   [:patience.model/address {:optional true} string?]
   [:patience.model/insurance-number {:optional true} int?]])

(def Patient
  ;; FIXME empty strings should be coerced to nil
  [:map
   [:name {:optional true} string?]
   [:gender {:optional true} string?]
   ;; FIXME inst? convertion to java.util.Date timestamp with local tz. To prevent timezones problems probably LocalDate
   ;; should be used. Take a look on malli transformes.
   [:date-of-birth {:optional true} [:or inst? empty?]]
   [:address {:optional true} string?]
   [:insurance-number {:optional true} [:or int? empty?]]])

;; FIXME split api routes and hiccup ui routes
(defn routes [patients]
  [["/patients" {:get {:handler (partial views/patients {:patients patients})
                       :parameters {:query [:map
                                            [:filter {:optional true} string?]
                                            [:page {:optional true} pos-int?]]}}
                 :post {:handler (partial views/create-patient {:patients patients})
                        :parameters {:form Patient}}}]
   ["/patients/:id" {:get {:handler (partial views/patient {:patients patients})
                           :parameters {:path {:id pos-int?}}}
                     :post {:handler (partial views/save-patient {:patients patients})
                            :parameters {:path [:map [:id pos-int?]]
                                         :form Patient}}
                     :delete {:handler (partial views/delete-patient {:patients patients})
                              :parameters {:path [:map [:id pos-int?]]}}}]
   ["/api" {:name ::api
            :middleware [muuntaja/format-middleware]}
    ["/ping" {:name ::ping
              :get http-api/pong}]
    ["/patients" ::patients-view
     ["" {:get (partial http-api/list-patients patients)
          :post {:handler (partial http-api/create-patient! patients)
                 :parameters {:form-params ApiPatient}}}]
     ["/:id" {:name ::patient-view
              :parameters {:path {:id pos-int?}}}
      ["" {:get (partial http-api/get-patient patients)
           :put {:handler (partial http-api/update-patient! patients)
                 :parameters {:body ApiPatient}}
           :delete (partial http-api/delete-patient! patients)}]]]]])

(defn router [patients]
  (ring/router (routes patients)
               {; uncomment to debug middleware step by step
                #_#_:reitit.middleware/transform dev/print-request-diffs
                :data {:muuntaja (m/create (assoc m/default-options
                                                  :default-format "application/json"))
                       :coercion rcm/coercion
                       :middleware [rrc/coerce-exceptions-middleware
                                    rrc/coerce-request-middleware
                                    rrc/coerce-response-middleware]}
                ;; FIXME deal with coercion compiling
                #_#_:compile coercion/compile-request-coercers}))

(defn default-handler
  [serve-static?]
  (if serve-static?
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler)))

;; FIXME do not recreate app on every request for prod
(defn app
  ([patients] (app patients true))
  ([patients serve-static?]
   (ring/ring-handler
    (router patients)
    (ring/routes (default-handler serve-static?))
    {:middleware [parameters/parameters-middleware
                  wrap-hidden-method
                  muuntaja/format-middleware]})))

(comment
  (def patients (patience.db/patients :dummy))

  ((app patients)
   {:request-method :get
    :uri "/api/patients"})

  ((app patients)
   {:request-method :get
    :uri "/api/ping"})

  ((app patients)
   {:request-method :get
    :uri "/"})

  (require '[reitit.core :as r])

  (r/match-by-path (router patients) "/api/patient")
  (r/match-by-path (ring/get-router (app patients)) "/")
  (def compiled (-> (app patients) ring/get-router r/compiled-routes))
  (require '[clojure.data])
  (clojure.data/diff (-> (app patients) ring/get-router r/compiled-routes) compiled))
