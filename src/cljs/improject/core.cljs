(ns improject.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [re-frame.core :refer [subscribe dispatch-sync dispatch]]
              
              [improject.state :as state]
              [improject.handlers :as handlers]
              [improject.login :refer [login-view]]
              [improject.registerationform :refer [registeration-form]]))
 
;; -------------------------
;; Views

(defn initial-view []
  ;; (dispatch-sync [:reset-location])
  (let [location (subscribe [:location])
        username (subscribe [:username])]
    (if (nil? @location)
      (dispatch-sync [:reset-location]))
    
    (fn []
      [:div 
       [:button#reset {:on-click #(dispatch [:reset-location])} "Reset"]
       
      (case @location
        :login [login-view]
        :register [:div
                   [:h2 "Register"]
                   [registeration-form]
                   [:p "After registration, admins will be notified of your registration. After they have accepted you, you'll be notified and can log in"]]
        [:div "Bugaa, location is " (str @location)])])))

(defn home-page []
  [:div [initial-view]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))


;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
