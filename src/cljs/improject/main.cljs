(ns improject.main
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(defn friend-cell [friend & {:keys [own?] :or {own? false}}]
  [(if own?
      :div.friend-cell.flex.own
      :div.friend-cell.flex)
   {:on-click (if own?
                #()
                #(dispatch [:open-conv (:username friend)]))}
   [:div.friend-img
    [:img {:src (:img_location friend)
           :alt (str "Image of " (:displayname friend))}]]
   [:div
   [:h2.friend-name (:displayname friend)]
    [:p.personal-message (:personal_message friend)]]])

(defn main-view []
  (let [friend-list (subscribe [:friend-list])
        user (subscribe [:user-model])]
    (fn []
      (let [toret (->> @friend-list
                       (map friend-cell)
                       vec
                       (into [:div#friend-list]))]
        [:div         
         [:div
          [:p "You have to allow pop-ups because conversations are opened in their own tabs"]
          [friend-cell @user :own? true]
          
          toret]]))))
