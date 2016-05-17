(ns improject.handler
  (:require [compojure.core :refer [POST GET defroutes]]
            [figwheel-sidecar.repl-api :as s]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [improject.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [korma.core :as k]
            [clojure.edn :refer [read-string]]
            [clojure.string :as str]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [clojure.pprint :refer [pprint]]
            [schema.core :as schemas]

            [improject.db :refer [users font_preference friendship]]
            [improject.schemas :refer [sanitized-user-schema
                                       user-schema
                                       login-schema
                                       message-schema
                                       session-user-schema
                                       session-id-schema]]
            [improject.serialization :refer [save-user! get-friends-of! friends?
                                             send-friend-request! friend-requests!
                                             remove-request! approve-request!]]
            [improject.security :refer [sha-512]]
            [improject.inboxes :refer [send-to! in? inbox-of!]]

            [clojure.core.async :as a]
            [clj-time.core :as t]))

(defn sanitize-user
  "Sanitizes user-objects for sending to user. Sanitization consists of removing admin-, can_login-, password-, id- and font_id - flags"
  [u]
  (-> u
      (dissoc :can_login :id :font_id :password)))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure where ks is a
  sequence of keys and returns a new nested structure."
  {:static true}
  [m [k & ks]]
  (if ks
    (assoc m k (dissoc-in (get m k) ks))
    (dissoc m k)))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(def loading-page
  (html5
   [:head
     [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))]
    [:body
     mount-target
     (include-js "/js/app.js")]))

(defn loading-page-with-js [js]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
    [:script js]]
    [:body
     mount-target
     (include-js "/js/app.js")]))

(def infernal-error {:status 500
                     :headers {"Content-Type" "text/plain"}
                     :body "Infernal Server Error"})

(def success {:status 200
              :headers {"Content-Type" "text/plain"}})

(defmacro with-validation [[obj form schema] & forms]
  `(try
     (let [~obj (schemas/validate ~schema ~form)]
       ~@forms)
     (catch Exception ex#
       (pprint ex#)
       infernal-error)))

;; username - list-of-gensym'd-keywords
(def session-ids (atom {}
                       :validator (partial schemas/validate session-id-schema)))

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  (GET "/no-users" []
       (let [users (k/select users)]
         {:status 200
          :headers {"Content-Type" "text/plain"}
          :body (if (empty? users)
                  "true"
                  "false")}))
  (GET "/friends-of/:user"
       {{user :user} :params
        {username :username :as session} :session}

       (if (= user username)
         {:status 200
          :headers {"Content-Type" "text/plain"}
          :body (pr-str (get-friends-of! user))}
         infernal-error))
  (POST "/user"
        {{friend-username :friend
          user :user} :params
          {username :username} :session}
        (if (= user username)
          (let [friends (->> (get-friends-of! user)
                             (filter #(= (:username %) friend-username))
                             (map sanitize-user))]
            (if (= (count friends) 1)
              (with-validation [frst (first friends) sanitized-user-schema]
                (assoc success :body (pr-str frst)))
              (assoc infernal-error :body (if (< (count friends) 1)
                                                 "Too few friends"
                                                 "Too many friends"))))
          (do
            (println "(= " user " " username")")
            infernal-error)))
  (POST "/inbox" {{username :username
                   session-id :sessionid
                   with-whom :friend-name :as params} :params
                   {session-username :username :as session} :session}
        (let [session-id (-> session-id
                             (str/replace #":" "")
                             keyword)]
          (if (and (= username session-username)
                   (contains? @session-ids username)
                   (in? (get @session-ids username) session-id))
            (-> success
                (assoc :body (-> (inbox-of! username with-whom
                                            session-id session-ids)
                                 pr-str)))
            (do
              (println "(= username session-username) ? "
                       (if (= username session-username)
                         "true"
                         "false"))
              (println "(contains? @session-ids username)"
                       (if (contains? @session-ids username)
                         "true"
                         "false"))
              (println "(in? (get @session-ids username) session-id)"
                       (if (in? (get @session-ids username) session-id)
                         "true"
                         "false"))
              infernal-error))))
  (POST "/send-message"
        {{model :model
          recipient :recipient :as params} :params
          {username :username :as session} :session}
        (def repl-message model)
        (println "Class of model " (class model))
        (with-validation [model
                          (-> model
                              read-string
                              (assoc :date (.toDate (t/now))
                                     :sent-to []
                                     :recipient recipient))
                          message-schema]
          (send-to! recipient model)
          (send-to! username (assoc model :recipient username))
          (assoc success :body "Sending succeeded")))

  (GET "/users/:username/:filter"
       {{user :username
         filter :filter} :params
         {username :username :as session} :session}
       (if (= user username)
         (let [user-set (->> (k/select users
                                       (k/where (or {:username [like (str "%" filter "%")]}
                                                    {:displayname [like (str "%" filter "%")]})))
                             (map sanitize-user)
                             vec)]
           (assoc success :body (pr-str user-set)))
         infernal-error))                             

  (GET "/users/:username"
       {{user :username} :params
        {username :username :as session} :session}
       (if (= user username)
         (let [all-users (k/select users)
               requesters (->> all-users
                               (filter #(= (:username %) username)))]
           (if (and (= 1 (count requesters))
                    (:admin (first requesters)))
             (assoc success :body (pr-str (vec all-users)))
             infernal-error))
         infernal-error))
            
        

  (GET "/sid/:sid/conversation/:friend"
       {{friend :friend
         session-id :sid} :params
         {username :username :as session} :session}
       (let [users (->>
                    (k/select users
                              (k/with font_preference)
                              (k/where (= username :username)))
                    (map sanitize-user))]
         (if-not (empty? users)
           (with-validation [frst (-> users
                                      first
                                      (assoc :sessionid (keyword (str/replace
                                                                  session-id
                                                                  #":"
                                                                  ""))))
                             session-user-schema]
             (loading-page-with-js
              (str "var usermodel = \"" (str/replace (pr-str frst) #"\"" "\\\\\"") "\";")))
           infernal-error)))

  (POST "/login" {{edn :edn} :params
                  session :session}
        (println "Session: ")
        (pprint session)
        (with-validation [login-model
                          (-> edn read-string (update-in [:password] sha-512))
                          login-schema]
          (let [users (k/select users
                                (k/with font_preference)
                                (k/where login-model))]
            (if (= (count users) 1)
              (let [session-id (keyword (gensym))]
                (with-validation [user (-> users
                                           first
                                           (assoc :sessionid session-id)
                                           (dissoc :password :can_login
                                                   :id :font_id))
                                  session-user-schema]
                  (print "Logged in")
                  (pprint user)

                  (when (nil? (get @session-ids (:username user)))
                    (swap! session-ids assoc (:username user) []))
                  (swap! session-ids update (:username user) conj session-id)
                  
                  {:status 200
                   :session (assoc session :username (:username login-model))
                   :body (pr-str {:success? true
                                  :data (pr-str user)})}))
              (do
                (println  "Logging in for " (:username login-model) " failed. These users found: ")
                (pprint users)
                {:status 200
                 :body (pr-str {:success? false
                                :data ""})})))))

  (GET "/request-friend/:username/:friend-name"
       {{username :username
         friend-name :friend-name} :params
         {session-username :username} :session}
       (if (= username session-username)
         (if-not (friends? username friend-name)
           (send-friend-request! username friend-name)
           (do
             (println "Already friends")
             infernal-error))
         infernal-error))

  (GET "/friend-requests/:username"
       {{username :username} :params
        {session-username :username} :session}
       (if (= session-username username)
         (assoc success :body
                (pr-str (friend-requests! username)))
         infernal-error))

  (GET "/approve-friend/:username/:friend-name"
       {{username :username
         friend :friend-name} :params
        {session-username :username} :session}
       (if (= session-username username)
         (do
           (approve-request! friend username)
           (assoc success :body "success"))
         infernal-error))

  (GET "/remove-request/:username/:friend-name"
       {{username :username
         friend :friend-name} :params
        {session-username :username} :session}
       (if (= session-username username)
         (do
           (remove-request! friend username)
           (assoc success :body "success"))
         infernal-error))       
       
  (POST "/register-user" {{edn :edn} :params}        
        (with-validation [{username :username
                           :as user} ;;destructuring
                          (-> edn
                              read-string
                              (update-in [:password] sha-512)
                              (assoc :admin false)) ;;parsing the incoming edn
                          user-schema ;;schema
                          ]
          (let [interesting-users (k/select users
                                            (k/where {:username username}))
                initial-registration? (if (empty? interesting-users)
                                        (empty? (k/select users))
                                        false)]
            (if (empty? interesting-users)
              (do
                (save-user!
                 (if initial-registration?
                   (assoc user :can_login true :admin true)
                   (assoc user :can_login true)))
                {:status 200
                 :body "true"})
              (throw (Exception. (str "User " username " exists already")))))))
  
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
