(ns patience.views
  (:require
   [hiccup.core :as hiccup]
   [patience.db :as db]
   [patience.filter :as filter]
   [patience.model :as-alias m]
   [ring.util.http-response :as http]
   [shadow.css :refer [css]]))

(defn- page
  "Wrap page body with html page structure."
  [& body]
  (hiccup/html
   [:html {:lang "en"}
    [:head
     [:link {:rel "stylesheet" :href "/css/ui.css"}]
     [:title "Patience"]]
    (into [:body ] body)]))

(defn- patients-page [patients filter-str]
  (let [th-col (css :px-6 :py-3)
        td (css :px-6 :py-4)]
    (page
     [:style
".tooltip:hover .tooltiptext {
  visibility: visible;
}"]
     [:form {:class (css :bg-slate-900 :shadow-2xl :flex :justify-between)}
      [:div.tooltip {:class (css :relative :inline-block :w-5of6 :flex-none :px-2)}
       [:input {:class (css :w-full :bg-slate-300 :my-2 :border-2 :border-rose-600 :text-rose-600)
                :id :filter
                :name :filter
                :maxlength 128
                :value filter-str
                :placeholder ":name/eq \"Ivan Ivanov\" :date-of-birth/lt 2022-01-01 :date-of-birth/gt 2021-12-01 \"oh !@#$\""}]
       [:span.tooltiptext {:class (css :m-2 :bg-amber-50 :shadow-md :text-sm :rounded-md :p-2 :text-gray-500 :z-10
                                       :absolute :invisible {:top "100%" :left "0%"})}
        [:div
         "To search by patient name - type any word. Advanced filters consist patient field key, that may be either "
         [:span {:class (css :text-rose-500)} "name"] ", "
         [:span {:class (css :text-rose-500)} "date-of-birth"] ", "
         [:span {:class (css :text-rose-500)} "gender"] ", "
         [:span {:class (css :text-rose-500)} "address"] " and "
         [:span {:class (css :text-rose-500)} "insurance-id"] " prefixed by "
         [:span {:class (css :text-rose-500)} ":"] " plus patient predicate key "
         [:span {:class (css :text-rose-500)} "eq"] ", "
         [:span {:class (css :text-rose-500)} "like"] ", "
         [:span {:class (css :text-rose-500)} "gt"] " and "
         [:span {:class (css :text-rose-500)} "lt"] " separated by "
         [:span {:class (css :text-rose-500)} "/"] " and any value. Value could be quoted to support passing spaces."]
        [:div
         [:span "Example: "]
         [:span {:class (css :text-rose-500)}  "':name/eq \"Ivan Ivanov\" :date-of-birth/lt 2022-01-01 :date-of-birth/gt 2021-12-01'"]]]]
      [:button {:class (css :w-1of6 :font-bold :text-rose-600 :border-2 :border-rose-600 :m-2 :px-2)} "Filter"]]
     [:div {:class (css :pl-2 :bg-gray-50)}
      [:div {:class (css :flex :justify-between :border-b)}
       [:h1 {:class (css :uppercase :font-bold :text-gray-900 :px-6 :py-4 :text-xl)}
        "Patients"]
       [:form {:method :post}
        [:input {:type :hidden
                 :name :name
                 :value "New Patient"}]
        [:button
         [:img {:src "/media/plus.svg"
                :class (css {:width "20px" :height "20px"}
                            :mx-6 :my-5)}]]]]
      [:table {:class (css :table-fixed :w-full :text-left :text-sm :text-gray-500)}
       [:thead {:class (css :text-xs :text-gray-700 :uppercase :bg-gray-50)}
        [:tr
         [:th {:class th-col :scope :col} "Name"]
         [:th {:class th-col :scope :col} "Gender"]
         [:th {:class th-col :scope :col} "Date of Birth"]
         [:th {:class th-col :scope :col} "Address"]
         [:th {:class th-col :scope :col} "Insurance Number"]
         [:th {:class (css :w-5)}]]]
       (into [:tbody]
             (map (fn [{:patience.model/keys [id name gender date-of-birth address insurance-number]}]
                    [:tr {:class (css :border-b)}
                     [:th {:scope :row
                           :class (css :px-6 :py-4 :font-medium :text-gray-900 :whitespace-nowrap)}
                      [:a {:href (str "patients/" id)} name]]
                     [:td {:class td} gender]
                     [:td {:class td} date-of-birth]
                     [:td {:class td} address]
                     [:td {:class td} insurance-number]
                     [:td {:class (css :mx-6)}
                      [:form {:action (str "/patients/" id) :method :post}
                       [:input {:type :hidden
                                :name :_method
                                :value :delete}]
                       [:button
                        [:img {:src "/media/delete.svg"
                               :class (css {:width "20px" :height "20px"})}]]]]])
                  patients))]])))

(defn- row [{:keys [type locator patient] :or {type 'text}} label]
  [:div {:class (css :flex :px-2 :pt-1)}
   [:label {:class (css :w-2of6)
            :for locator}
    label]
   [:input {:class (css :w-4of6 :border-solid :border-2)
            :name locator
            :id locator
            :type type
            :value (get patient locator)}]])

(defn- patient-form [patient]
  [:form {:method :post}
   (row {:patient patient :locator ::m/name} "Name")
   (row {:patient patient :locator ::m/gender} "Gender")
   (row {:patient patient :locator ::m/date-of-birth :type 'date} "Date Of Birth")
   (row {:patient patient :locator ::m/address} "Address")
   (row {:patient patient :locator ::m/insurance-number :type 'number} "Insurance number")
   [:button {:class (css :border-solid :border-2 :border-slate-900 :bg-sky-300 :m-2 :px-2 :shadow-xl)}
    "Save"]])

(defn- breadcrumb [id]
  [:div {:class (css :bg-slate-900 :text-rose-600 :pl-2)}
   [:a {:href "/patients"}
    [:span "Patients"]]
   [:span {:class (css :px-2)} "/"]
   [:span id]])

(defn- patient-page [patient id]
  (page
   (breadcrumb id)
   [:div {:class (css :m-10 :flew :bg-slate-50 :rounded-br-lg :rounded-bl-lg :shadow-xl)}
    [:div {:class (css :flex :justify-between :rounded-tr-lg :rounded-tl-lg :bg-slate-900 :pt-2 :px-2)}
     [:h1 {:class (css :font-bold :text-rose-600)}
      "Patient"]
     [:form {:method :post}
      [:input {:type :hidden
               :name :_method
               :value :delete}]
      [:button
       [:img {:src "/media/delete.svg"
              :class (css {:width "20px" :height "20px"})}]]]]
    (patient-form patient)]))

;; FIXME probably should be moved to coercion level
(defn- normalize-patient
  "Add patience.model namespace to patient map, replace empty strings with nils. "
  ([patient] (normalize-patient nil patient))
  ([id patient]
   (cond-> (reduce-kv (fn [acc k v]
                        (assoc acc (keyword "patience.model" (name k)) (when-not (and (string? v)
                                                                                      (empty? v))
                                                                         v)))
                       {}
                       patient)
     id (assoc ::m/id id))))

;; API
(defn patient [{:keys [patients]} {{{:keys [id]} :path} :parameters}]
  (let [patient (db/get-patient patients id)]
    (if (seq patient)
      (http/ok (patient-page patient id))
      (http/not-found))))

(defn patients [{:keys [patients]} {{{filter-str :filter} :query} :parameters}]
  (let [filters (filter/parse-filters filter-str)
        patients (db/list-patients patients filters)]
    (http/ok (patients-page patients filter-str))))

(defn save-patient [{:keys [patients]} {{{:keys [id]} :path patient :form} :parameters}]
  (let [new-state (normalize-patient id patient)]
    (when (db/update-patient! patients id new-state)
      (http/ok (patient-page (db/get-patient patients id) id)))))

(defn delete-patient [{:keys [patients]} {{{:keys [id]} :path} :parameters}]
  (when (db/delete-patient! patients id)
    (http/see-other "/patients")))

(defn create-patient [{:keys [patients]} {{patient :form :or {patient {}}} :parameters}]
  (let [patient (normalize-patient patient)]
    (when-let [{::m/keys [id]} (db/create-patient! patients patient)]
      (http/see-other (str "/patients/" id)))))
