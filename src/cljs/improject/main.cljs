(ns improject.main
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(defn friend-cell [friend & {:keys [own?] :or {own? false}}]
  [ (if own?
      :div.friend-cell.flex.own
      :div.friend-cell.flex)
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
        (.log js/console (str toret))
        [:div
         ;; [:div.friend-cell.own
         ;;  [:h2 "Logged in as " (:displayname @user)]]
         [friend-cell @user :own? true]
         
         toret]))))
