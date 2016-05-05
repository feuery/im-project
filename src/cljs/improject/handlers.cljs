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
     (assoc db :location :login)
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
                    (let [{:keys [success? data]} (read-string result)
                          data (read-string data)]
                      (if success?
                        (do
                          (.log js/console "Login succeeded")
                          (dispatch [:find-friends])
                          (assoc db :location :main
                                 :user-model data))
                        (do
                          (.log js/console "Login failed")
                          db)))))

(register-handler :reset-location
                  (fn [db [_ location :as params]]
                    (let [location (or location :login)]
                    (.log js/console (str "Params are " params))
                      (.log js/console (str ":reset-location to " location))
                            ;; (set-url "/login")
                            ;; (if-not (= (-> js/window .-location ) "
                      (assoc db :location location))))

(register-handler :register
                  (fn [db _]
                    ;; (set-url "/register")
                    (.log js/console ":register handled")
                    (assoc db :location :register)))

(register-handler :find-friends
                  (fn [db _]
                    (let [url (str "/friends-of/" (-> db :user-model :username))]
                      (GET url
                           {:error-handler #(dispatch [:bad-result %])
                            :handler #(dispatch [:friends-found %])})
                      db)))

(register-handler :friends-found
                  (fn [db [_ friends-result]]
                    (let [friend-list (or (read-string friends-result) [])]
                      (assoc db :friend-list friend-list))))

(register-handler :open-conv
                  (fn [db [_ friend-username]]
                    (.open js/window (str "/conversation/" friend-username) "_blank")
                    db))

(.log js/console "improject.handlers loaded")
