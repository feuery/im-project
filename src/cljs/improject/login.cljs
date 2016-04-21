(ns improject.login
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(defn login-view []
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
    
    [:button#loginbtn.column-three "Login"]]])

