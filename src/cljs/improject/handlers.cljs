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
   (js/alert response)
   db))

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
