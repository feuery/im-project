(ns improject.login
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(defn register-initial []
  [:div "Registering initial user"])

(defn login-view []
  (dispatch [:no-users?])
  (let [no-users (subscribe [:no-users])]
    (fn []
      (if @no-users
        [register-initial]
        [:div
         [:h4 "Login"]
         [:div#login_form.flex
          [:div.column-one
           [:input#username {:type "text"
                             :placeholder "Username"}]
           [:input#password {:type "password"
                             :placeholder "Password"}]]

          [:div#greeting.column-two
           "Welcome "
           [:br]
           [:br]
           "This is an experimental instant messenger. This'll be made with ClojureScript and Clojure. Written by" [:a {:href "http://yearofourlord.blogspot.com"} "Feuer"]]
          
          [:div.column-three
           [:p "Status of @no-users: " (if @no-users "true" "false")]
           [:button#loginbtn
            "Login"]]]]))))

