(ns improject.registerationform
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch]]))

(defn value-of [event]
  (-> event .-target .-value))

(defn value-of-checkbox [event]
  (-> event .-target .-checked))

(defn registeration-form []
  (let [viewstate (r/atom {:persmsg ""
                           :font-colour "#000000"
                           :font-underline? false
                           :font-name "'Helvetica Neue', Verdana, Helvetica, Arial, sans-serif"
                           :password ""
                           :font-bold? false
                           :username ""
                           :font-italic? false
                           :displayname ""})]
    (fn []
      [:div
       [:table#registeration-form
        [:tr
         [:td "Username " [:br] "(will be used for logging in. Can't be changed): "]
         [:td
          [:input#username {:type "text"
                            :maxLength 255
                            :on-change #(swap! viewstate assoc :username (value-of %))}]]]
        [:tr
         [:td "Password: "] [:td
                             [:input#password
                              {:type "password"
                               :on-change #(swap! viewstate assoc :password (value-of %))}]]]
        [:tr
         [:td "Personal message: "] [:td
                                     [:textarea#persmsg
                                      {:maxLength 2000
                                       :on-change #(swap! viewstate assoc :persmsg (value-of %))}]]]
        [:tr
         [:td "Displayname (will be shown to others. Can be changed): "]
         [:td
          [:input#displayname {:type "text" :maxLength 255
                               :on-change #(swap! viewstate assoc :displayname (value-of %))}]]]
        [:tr
         [:td "Font: "]
         [:td
          [:label "Name:" ][:input#font {:type "text"
                                         :defaultValue "'Helvetica Neue', Verdana, Helvetica, Arial, sans-serif"
                                         :on-change #(swap! viewstate assoc :font-name (value-of %))}]
          [:label "Bold"]
          [:input#bold {:type "checkbox"
                        :on-click (fn [event]
                                    (swap! viewstate assoc :font-bold? (value-of-checkbox event)))}]
          [:label "Italic"]
          [:input#italic {:type "checkbox"
                          :on-click (fn [event]
                                      (swap! viewstate assoc :font-italic? (value-of-checkbox event)))}]
          [:label "Underline"]
          [:input#underline {:type "checkbox"
                             :on-click (fn [event]
                                         (swap! viewstate assoc :font-underline? (value-of-checkbox event))
                                         nil)}]
          [:label "Colour"]
          [:input#colour {:type "color"
                          :on-change #(swap! viewstate assoc :font-colour (value-of %))}]]]]
       ;; [:div "Value of viewstate?" [:br] (with-out-str (cljs.pprint/pprint @viewstate))]
       [:button {:on-click #(dispatch [:register-user @viewstate])} "Register"]])))
                                 
