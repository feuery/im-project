(ns improject.handlers
  ;; (:require-macros [cljs.core.async.macros :refer [go]]) 
  (:require [re-frame.core :refer [register-handler
                                   dispatch]]
            [reagent.session :as session]
            [ajax.core :refer [GET POST]]
            [cljs.reader :refer [read-string]]

            [improject.formtools :refer [set-url]]
            ;; [cljs.core.async :refer [chan <! >!]]
            ))

(register-handler
 :no-users?
 (fn [db _]
   (GET "/no-users"
        {:handler #(dispatch [:user-count-status %1])
         :error-handler #(dispatch [:bad-result %1])})
   db))

(def error-handler #(dispatch [:bad-result %1]))

(register-handler :set-conversation-partner
                  ;; Call (dispatch-sync [:find-friends]) before this
                  (fn [db [_ partner-username]]
                    (js/console.log "Logged in username is " (get-in db [:user-model :username])) 
                    (POST "/user"
                          {:params {:friend partner-username
                                    :user (get-in db [:user-model :username])}
                           :format :url
                           :handler #(dispatch [:setting-conversation-partner %1])
                           :error-handler error-handler})
                    db))

(register-handler :setting-conversation-partner
                  (fn [db [_ friend-model]]
                    (let [friend (read-string friend-model)]
                      (assoc db :conversation-partner friend))))

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

(register-handler :send-message
                  (fn [db [_ friend-username message]]
                    (let [message-model (pr-str {:message message ;;contains font-data
                                                 :sender (:user-model db)})]
                      (POST "/send-message"
                            {:params {:model message-model
                                      :recipient friend-username}
                             :format :url
                             :handler #(.log js/console %)
                             :error-handler error-handler})
                      db)))

(register-handler :load-inbox
                  (fn [db _]
                    (.log js/console ":load-inbox")
                    (let [partner-name (get-in db [:conversation-partner :username])] 
                      (POST "/inbox"
                            {:params {:username (get-in db [:user-model :username])
                                      :friend-name partner-name
                                      :sessionid (get-in db [:user-model :sessionid])}
                             :format :url
                             :handler #(dispatch [:inbox-loaded %])
                             :error-handler error-handler})
                      db)))

(register-handler :inbox-loaded
                  (fn [db [_ inbox-str]]
                    (let [inbox (read-string inbox-str)]
                      (js/setTimeout #(dispatch [:load-inbox]) 1000)
                      (if (nil? (:inbox db))
                        (assoc db :inbox inbox)
                        (update db :inbox (comp vec concat) inbox)))))

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

(register-handler :set-user-model
                  ;; Do not call this unless server has first validated that session contains a valid username
                  (fn [db [_ usermodel]]
                    (let [um (read-string usermodel)]
                      (.log js/console (str "at :set-user-model, um is " um))
                      (assoc db :user-model um))))

(register-handler :reset-location
                  (fn [db [_ location :as params]]
                    (let [location (or location :login)]
                    (.log js/console (str "Params are " params))
                      (.log js/console (str ":reset-location to " location))
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

(register-handler :get-all-users
                  (fn [db _]
                    (when (-> db :user-model :admin)
                      (GET (str "/users/" (-> db :user-model :username))
                           {:error-handler error-handler
                            :handler #(dispatch [:users-arrived %])}))
                    db))

(register-handler :search-friends
                  (fn [db [_ username-filter]]
                    (if-not (= username-filter "")
                      (GET (str "/users/" (-> db :user-model :username) "/" username-filter)
                         {:error-handler error-handler
                          :handler #(dispatch [:filtered-users-arrived %])})
                      (dispatch [:filtered-users-arrived "[]"])) ;; "[]" to clear the results with an empty set
                    db))

(register-handler :filtered-users-arrived
                  (fn [db [_ users-str]]
                    (let [users (read-string users-str)]
                      (assoc db :filtered-users users))))

(register-handler :users-arrived
                  (fn [db [_ users-str]]
                    (let [users (read-string users-str)]
                      (assoc db :all-users users))))

(register-handler :friends-found
                  (fn [db [_ friends-result]]
                    (let [friend-list (or (read-string friends-result) [])]
                      (.log js/console (str "Found friends: " friend-list))
                      (assoc db :friend-list friend-list))))

(register-handler :open-conv
                  (fn [db [_ friend-username]]
                    (let [sid (get-in db [:user-model :sessionid])]
                      (.open js/window (str "/sid/" sid "/conversation/" friend-username) "_blank")
                      (.log js/console (str "Opened conversation with " friend-username))
                      db)))

(register-handler :show-admin-gui
                  (fn [db _]
                    (if (-> db :user-model :admin)
                      (assoc db :location :admin-gui)
                      (do
                        (.log js/console (str "No admin-gui for non-admins (" (pr-str (:user-model db))")"))
                        db))))


(.log js/console "improject.handlers loaded")
