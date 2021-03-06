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
              [improject.main :refer [main-view]]
              [improject.conversation :refer [conversation-page]]
              [improject.admin :refer [admin-view]]
              [improject.friend_search :refer [friend-search]]
              [improject.formtools :refer [value-of in?]]))
 
;; -------------------------
;; Views

(defn bugaa [location]
  [:div "Bugaa, location is " (str location)])

(defn nav [admin?]
  (fn []
    [:nav
     [:ul
      [:li [:button {:style {:display (if admin? ;;check on the server side too
                                        :inline
                                        :none)}
                     :on-click #(dispatch [:show-admin-gui])} "Admin tools"]]
      [friend-search]]]))

(defn request [{requester :username1 approved :approved}]
  (let [friend-list (subscribe [:friend-list])]
    (fn []
      (if (in? @friend-list requester)
        [:li]
        [:li
         [:h2.friend-name requester]
         [:p.personal-message " would like to be your friend."]
         [:button.accept-rq {:on-click #(dispatch [:make-friend requester])}
          "Accept request"]
         [:button {:on-click #(dispatch [:remove-request requester])}
          "Remove request"]]))))

(defn header []
  (let [requests (subscribe [:requests])
        show-requests? (subscribe [:show-requests?])]
    (dispatch [:get-requests])
    (fn []
      (let [header-elem
            [:header
             [:div#requests
              {:on-click #(do
                            (dispatch [:toggle-request-visibility]))}
              (str (count @requests) " friend requests")]
             ;; [:p "show-requests? " (if @show-requests? "true" "false")]
             ]]
        (if @show-requests?
          (let [result (->> @requests
                            (map (fn [r]
                                   [request r]))
                            (into [:ul.request-div]))]
            [:div
             header-elem
             result])
          header-elem)))))
               

(defn initial-view []
  (let [location (subscribe [:location])
        user (subscribe [:user-model])]    
    (fn []
      [:div 
       [:button#reset {:on-click #(dispatch [:reset-location :main])} "Reset"]
       
      (case @location
        :login [login-view]
        :register [:div
                   [:h2 "Register"]
                   [registeration-form]
                   [:p "After registration, admins will be notified of your registration. After they have accepted you, you'll be notified and can log in"]]

        :main [:div
               [header]
               [nav (:admin @user)]
               [main-view]]
        :admin-gui [:div
                    [header]
                    [nav (:admin @user)]
                    [admin-view]]
        [bugaa @location])])))

(defn home-page []
  [:div [initial-view]])
         

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (dispatch-sync [:reset-location :login])
  (.log js/console "In initial-page")
  (session/put! :current-page #'home-page))

(secretary/defroute "/sid/:sid/conversation/:friend" [session-id friend]
  ;; we need our user-model (img and font settings and all)
  ;; we need recipient's name for message dispatch
  ;; we need recipient's model for UI  
  (dispatch-sync [:reset-location :conversation])
  (dispatch-sync [:set-user-model js/usermodel])
  ;; (dispatch-sync [:find-friends]) ;; this fucker makes an Ajax-call, which leads to :set-conversation-partner being called before friend-list is populated
  (dispatch-sync [:set-conversation-partner friend])
  (dispatch [:load-inbox])
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
