(ns improject.handlers
  (:require [re-frame.core :refer [register-handler
                                   dispatch]]
            [ajax.core :refer [GET POST]]))

(register-handler
 :no-users?
 (fn [db _]
   (GET "/no-users"
        {:handler #(dispatch [:user-count-status %1])
         :error-handler #(dispatch [:bad-user-count-status %1])})
   db))

(register-handler
 :user-count-status
 (fn [db [_ response]]
    (assoc db :no-users (case response
                          "true" true
                          false))))

(register-handler
 :bad-user-count-status
 (fn [db [_ response]]
   (js/alert response)
   db))
