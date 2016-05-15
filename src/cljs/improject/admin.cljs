(ns improject.admin
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(defn admin-view []
  (let [all-users (subscribe [:all-users])]
    (dispatch [:get-all-users])
    (fn []
      [:div [:h2 "Welcome to admin view"]
             [:h3 "Registered users"]
             (->> @all-users
                  (map (fn [u]
                         [:li
                          [:p (:username u) " - " (:displayname u)]]))
                  (into [:ul]))])))
