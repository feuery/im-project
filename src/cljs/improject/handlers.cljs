(ns improject.handlers
  (:require [re-frame.core :refer [register-handler
                                   dispatch]]
            [reagent.session :as session]
            [ajax.core :refer [GET POST]]
            [cljs.reader :refer [read-string]]

            [improject.formtools :refer [set-url]]))

(register-handler
 :no-users?
 (fn [db _]
   (GET "/no-users"
        {:handler #(dispatch [:user-count-status %1])
         :error-handler #(dispatch [:bad-result %1])})
   db))

(register-handler
 :user-count-status
 (fn [db [_ response]]
   ;; (js/alert response)
   (assoc db :no-users (case response
                         "true" true
                         false))))

(register-handler
 :bad-result
 (fn [db [_ response]]
   (if (= response "Timeout")
     (dispatch [:reset-location])
     (do 
       (js/alert response)
       db))))

(register-handler
 :register-user
 (fn [db [_ user-val]]
   (POST "/register-user"
         {:params {:edn (prn-str user-val)}
          :format :url
          :handler #(dispatch [:registered %])
          :error-handler #(dispatch [:bad-result %1])})
   db))

(register-handler :registered
                  (fn [db [_ result]]
                    (if (= result "true")
                      (do
                        (js/alert "Registration succeeded. You'll be redirected to the login page")
                        (assoc db :no-users false
                               :location :login))
                      (do
                        (.log js/console (str "Result " result))
                        db))))

(register-handler :login
                  (fn [db [_ login-vm]]
                    (POST "/login"
                          {:params {:edn (prn-str login-vm)}
                           :format :url
                           :handler #(dispatch [:loggedin %])
                           :error-handler #(dispatch [:bad-result %])})
                    db))

(register-handler :loggedin
                  (fn [db [_ result]]
                    (if (= result "success")
                      (assoc db :location :main) 
                      (do
                        (.log js/console "Login failed")
                        db))))

(register-handler :reset-location
                  (fn [db _]
                    ;; (set-url "/login")  
                    (assoc db :location :login)
                    ))

(register-handler :register
                  (fn [db _]
                    ;; (set-url "/register")
                    (.log js/console ":register handled")
                    (assoc db :location :register)))

(.log js/console "improject.handlers loaded")
