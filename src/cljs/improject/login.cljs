(ns improject.login
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [improject.registerationform :refer [registeration-form]]
            [improject.formtools :refer [value-of value-of-checkbox return-clicked?]]))

(defn register-initial []
  [:div
   [:h2 "Welcome"]
   [:p "It seems there are no registered users at all. Let's create one. You'll be the admin who has the power to allow new users to login. First, though, I need some information:"]
   [registeration-form]])

(defn login! [login-viewmodel]
  (dispatch [:login login-viewmodel]))

(defn login-view []
  (dispatch [:no-users?])
  (let [no-users (subscribe [:no-users])
        login-viewmodel (r/atom {:username ""
                                 :password ""})]
    (fn []
      (if @no-users
        [register-initial]
        [:div
         [:h4 "Login"]
         [:div#login_form.flex
          [:div.column-one
           [:input#username {:type "text"
                             :placeholder "Username"
                             :on-change #(swap! login-viewmodel assoc :username (value-of %))}]
           [:input#password {:type "password"
                             :placeholder "Password"
                             :on-change #(swap! login-viewmodel assoc :password (value-of %))
                             :on-key-down #(if (return-clicked? %)
                                             (login! @login-viewmodel))}]]

          [:div#greeting.column-two
           "Welcome "
           [:br]
           [:br]
           "This is an experimental instant messenger. This'll be made with ClojureScript and Clojure. Written by" [:a {:href "http://yearofourlord.blogspot.com"} "Feuer"]]
          
          [:div.column-three
           ;; [:p "Status of @no-users: " (if @no-users "true" "false")]
           ;; [:p "Viewmodel: " (str @login-viewmodel)] 
           [:a {:href "#"
                :on-click #(dispatch [:register])} "Register"]
           [:button#loginbtn
            {:on-click #(login! @login-viewmodel)}
            "Login"]]]]))))

