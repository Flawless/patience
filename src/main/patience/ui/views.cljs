(ns patience.ui.views
  (:require
   [clojure.string :as s]
   [patience.model :as-alias m]
   [shadow.grove :as sg :refer [<< css defc]]))

(defn merge-classes [& classes]
  {:pre [(every? string? classes)]}
  (s/join " " classes))

(defc ui-filter-select []
  (bind {::m/keys [current-filter]}
    (sg/query-root
      [::m/current-filter]))

  (bind
    filter-options
    [{:label "All" :value :all}
     {:label "Active" :value :active}
     {:label "Completed" :value :completed}])

  (render
    (<< [:ul.filters
         (sg/keyed-seq filter-options :value
           (fn [{:keys [label value]}]
             (<< [:li [:a
                       {:class {:selected (= current-filter value)}
                        :ui/href (str "/" (name value))}
                       label]])))])))

(defc row [{:keys [class type patient-ident locator] :or {type 'text}} label]
  (event ::m/edit-update! [env _ e]
         (case (.-which e)
           13 ;; enter
           (.. e -target (blur))
           27 ;; escape
           (sg/run-tx env {:e ::m/cancel-edit-patient! :patient-ident patient-ident})
           ;; default do nothing
           nil))

  (bind {::m/keys [editing?] :as patient}
        (sg/query-ident patient-ident))

  (event ::m/edit-complete! [env _ e]
         (let [patient' (assoc patient locator (.. e -target -value))]
             (sg/run-tx env {:e ::m/save-patient! :patient patient'})))
  (render
   (<< [:div.row {:class (cond-> (css :flex :px-2 :pt-1)
                           (some? class) (merge-classes class))}
        [:label {:class (css :w-2of6)}
         label]
        [:div {:class (css :w-4of6)}
         (<< [:input {:class (css :w-full)
                      :on-keydown {:e ::m/edit-update!}
                      :on-blur {:e ::m/edit-complete!}
                      :type type
                      :value (get patient locator)}])]])))

(defc patient-view [current-patient]
  (render
   (<< [:form {:class (css :flex-none :w-3of5 :bg-slate-50 :rounded-br-lg :rounded-bl-lg :shadow-xl)}
        (row {:patient-ident current-patient
              :locator ::m/name} "Name")
        (row {:patient-ident current-patient
              :locator ::m/gender} "Gender")
        (row {:patient-ident current-patient
              :type 'date
              :locator ::m/date-of-birth} "Date of birth")
        (row {:patient-ident current-patient
              :locator ::m/address} "Addred")
        (row {:patient-ident current-patient
              :locator ::m/insurance-number} "Insurance number")])))

(defc patient-item [patient-ident]
  (bind {::m/keys [current-patient]}
        (sg/query-root [::m/current-patient]))

  (bind {::m/keys [name]}
        (sg/query-ident patient-ident))

  (render
   (if (= patient-ident current-patient)
     (<< [:li {:class (css :text-rose-900)
               :on-click {:e ::m/select-patient! :patient-ident patient-ident}}
          name])
     (<< [:li {:class (css :cursor-pointer [:hover :text-rose-900])
               :on-click {:e ::m/select-patient! :patient-ident patient-ident}}
          name]))))

(defc patients-list []
  (bind {::m/keys [filtered-patients]}
        (sg/query-root [::m/filtered-patients]))

  (render
   (<< [:ul.patients-list (sg/keyed-seq filtered-patients identity patient-item)])))

(defc stat-bar []
  (bind {::m/keys [patients-count]}
        (sg/query-root [::m/patients-count]))

  (render
   (<< [:div {:class (css :flex :max-w-4-xl :p-2 :bg-slate-900 :text-rose-600)}
        [:span {:class (css :flex-none :w-2of5)}
         "Total patients:"]
        [:span {:class (css :flex-none :w-3of5)}
         patients-count]])))

(defc ui-root []
  (bind {::m/keys [current-patient init-complete?]}
        (sg/query-root [::m/current-patient ::m/init-complete?]))

  (render
   (tap> [:curr-pat current-patient])
   (<< (stat-bar)
       [:div {:class (css :flex :max-w-4xl :px-2 :pt-2)}
        [:div {:class (css :flex :w-2of5 :justify-between :pt-2)}
         [:h1 {:class (css :flex-none :font-bold)} "Patients"]
         [:a {:href "#" :on-click {:e ::m/create-patient!}}
          [:img {:src "/media/plus.svg"
                 :class (css {:width "20px" :height "20px"})}]]]
        [:div {:class (css :flex :justify-between :w-3of5 :rounded-tr-lg :rounded-tl-lg :bg-slate-900 :pt-2 :px-2)}
         [:h1 {:class (css :font-bold :text-rose-600)}
          "Patient info"]
         (when current-patient
           (<< [:a {:href "#"
                    :on-click {:e ::m/delete-patient! :patient-ident current-patient}}
                [:img {:src "/media/delete.svg"
                       :class (css {:width "20px" :height "20px"})}]]))]]
       [:div {:class (css :flex :max-w-4xl :px-2)}
        [:div {:class (css :flex-none :w-2of5)}
         (if init-complete?
           (patients-list)
           (<< [:span {:class (css :text-slate-500 :font-medium :mb-3 :text-sm)}
                "Loading..."]))]
        (if current-patient
          (patient-view current-patient)
          (<< [:span {:class (css :text-slate-500 :font-medium :mb-3 :text-sm)}
               "Click on a patient in list"]))])))
