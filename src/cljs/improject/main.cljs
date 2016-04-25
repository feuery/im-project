(ns improject.main
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(defn friend-cell [friend]
  [:div.friend-cell.flex
   [:div.friend-img
    [:img {:src (:img_location friend)
           :alt (str "Image of " (:displayname friend))}]]
   [:div
   [:h2.friend-name (:displayname friend)]
   [:p.personal-message (:personal_message friend)]]])

(defn main-view []
  (let [friend-list (subscribe [:friend-list])]
    (fn []
      (let [toret (->> @friend-list
                        (map friend-cell)
                        vec
                        (into [:div#friend-list]))]
        (.log js/console (str toret))
        [:div
         [:div.friend-cell.own
          [:h2 "Who am I?"]]
         
         toret]))))
