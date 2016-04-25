(ns improject.handlers
  (:require [re-frame.core :refer [register-handler
                                   dispatch]]
            [reagent.session :as session]
            [ajax.core :refer [GET POST]]
            [cljs.reader :refer [read-string]]))

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
     (assoc db :sessionid -1
            :username "")
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
                        (.log js/console "success")
                        (assoc db :no-users false))
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
                      (assoc db :sessionid 1)
                      (do
                        (.log js/console "Login failed")
                        db))))

(register-handler :reset-location
                  (fn [db _] 
                    (assoc db :location :login)))

(register-handler :register
                  (fn [db _]
                    (-> (.-history js/window) (.pushState {} "Registering" "/register"))
                    (assoc db :location :register)))
                    
