(ns improject.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [re-frame.core :refer [subscribe dispatch-sync dispatch]]
              
              [improject.state :refer [data]]
              [improject.handlers :as handlers]
              [improject.login :refer [login-view]]
              [improject.registerationform :refer [registeration-form]]
              [improject.main :refer [main-view]]))
 
;; -------------------------
;; Views

(defn bugaa [location]
  [:div "Bugaa, location is " (str location)])

(defn initial-view []
  (let [location (subscribe [:location])]
    
    (fn []
      [:div 
       [:button#reset {:on-click #(dispatch [:reset-location :main])} "Reset"]
       
      (case @location
        :login [login-view]
        :register [:div
                   [:h2 "Register"]
                   [registeration-form]
                   [:p "After registration, admins will be notified of your registration. After they have accepted you, you'll be notified and can log in"]]
        :main [main-view]
        [bugaa @location])])))

(defn home-page []
  [:div [initial-view]])

(defn conversation-page []
  (let [location (subscribe [:location])
        conv-partner (subscribe [:conversation-partner])
        me (subscribe [:username])
        ]
    (fn []
      [:div [:button#reset {:on-click #(dispatch [:reset-location :conversation])} "Reset"]
       (case @location
         :conversation [:div
                        [:div (str "Conversation with " (pr-str @conv-partner))] 
                        [:div (str "You're " @me)]
                        [:div (str "Usermodel is " js/usermodel)]]
         [bugaa @location])
       ])))
         

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (dispatch-sync [:reset-location :login])
  (.log js/console "In initial-page")
  (session/put! :current-page #'home-page))

(secretary/defroute "/conversation/:friend" [friend]
  ;; we need our user-model (img and font settings and all)
  ;; we need recipient's name for message dispatch
  ;; we need recipient's model for UI
  
  (dispatch-sync [:reset-location :conversation])
  (dispatch-sync [:set-user-model js/usermodel])
  ;; (dispatch-sync [:find-friends]) ;; this fucker makes an Ajax-call, which leads to :set-conversation-partner being called before friend-list is populated
  (dispatch-sync [:set-conversation-partner friend])
  (.log js/console "In conversation-page")
  (session/put! :current-page #'conversation-page))


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
