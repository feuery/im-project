(ns improject.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [re-frame.core :refer [subscribe]]
              
              [improject.state :as state]
              [improject.login :refer [login-view]]))

;; -------------------------
;; Views

(defn initial-view []
  (let [sessionid (subscribe [:sessionid])
        username (subscribe [:username])]
    (fn []
      (if-not (pos? @sessionid)
        [login-view]
        [:div "Bugaa"]))))

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
