(ns improject.registerationform
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch]]))

(defn value-of [event]
  (-> event .-target .-value))

(defn value-of-checkbox [event]
  (-> event .-target .-checked))

(defn registeration-form []
  (let [viewstate (r/atom {:personal_message ""
                           :color "#000000"
                           :underline false
                           :font_name "'Helvetica Neue', Verdana, Helvetica, Arial, sans-serif"
                           :password ""
                           :bold false
                           :username ""
                           :italic false
                           :displayname ""
                           :img_location ""})]
    (fn []
      [:div
       [:table#registeration_form
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
                                     [:textarea#personal_message
                                      {:maxLength 2000
                                       :on-change #(swap! viewstate assoc :personal_message (value-of %))}]]]
        [:tr
         [:td "Displayname (will be shown to others. Can be changed): "]
         [:td
          [:input#displayname {:type "text" :maxLength 255
                               :on-change #(swap! viewstate assoc :displayname (value-of %))}]]]
        [:tr
         [:td "Location of your image: "]
         [:td
          [:input#imgurl {:type "text"
                          :on-change #(swap! viewstate assoc :img_location (value-of %))}]]]
        
        [:tr
         [:td "Font: "]
         [:td
          [:label "Name:" ][:input#font {:type "text"
                                         :defaultValue "'Helvetica Neue', Verdana, Helvetica, Arial, sans-serif"
                                         :on-change #(swap! viewstate assoc :font_name (value-of %))}]
          [:label "Bold"]
          [:input#bold {:type "checkbox"
                        :on-click (fn [event]
                                    (swap! viewstate assoc :bold (value-of-checkbox event)))}]
          [:label "Italic"]
          [:input#italic {:type "checkbox"
                          :on-click (fn [event]
                                      (swap! viewstate assoc :italic (value-of-checkbox event)))}]
          [:label "Underline"]
          [:input#underline {:type "checkbox"
                             :on-click (fn [event]
                                         (swap! viewstate assoc :underline (value-of-checkbox event))
                                         nil)}]
          [:label "Colour"]
          [:input#colour {:type "color"
                          :on-change #(swap! viewstate assoc :color (value-of %))}]]]]
        [:div "Value of viewstate?" [:br] (pr-str @viewstate)]
       [:button {:on-click #(dispatch [:register-user @viewstate])} "Register"]])))
                                 
