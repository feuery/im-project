(ns improject.handler
  (:require [compojure.core :refer [POST GET defroutes]]
            [figwheel-sidecar.repl-api :as s]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [improject.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [korma.core :as k]
            [clojure.edn :refer [read-string]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [clojure.pprint :refer [pprint]]
            [schema.core :as schemas]

            [improject.db :refer [users]]
            [improject.schemas :refer [user-schema login-schema]]
            [improject.serialization :refer [save-user! get-friends-of!]]
            [improject.security :refer [sha-512]]))

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

(def infernal-error {:status 500
                     :headers {"Content-Type" "text/plain"}
                     :body "Infernal Server Error"})

(defmacro with-validation [[obj form schema] & forms]
  `(try
     (let [~obj (schemas/validate ~schema ~form)]
       ~@forms)
     (catch Exception ex#
       (pprint ex#)
       infernal-error)))

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

  (POST "/login" {{edn :edn} :params
                  session :session}
        (println "Session: ")
        (pprint session)
        (with-validation [login-model
                          (-> edn read-string (update-in [:password] sha-512))
                          login-schema]
          (let [users (k/select users
                                (k/where login-model))]
            (if (= (count users) 1)
              (do
                (print "Logged in")
                (pprint (first users))
                {:status 200
                 :session (assoc session :username (:username login-model))
                 :body "success"})
              (do
                (println  "Logging in for " (:username login-model) " failed. These users found: ")
                (pprint users)
                {:status 200
                 :body "failure"})))))
                 

  (POST "/register-user" {{edn :edn} :params}        
        (with-validation [{username :username
                           :as user} ;;destructuring
                          (-> edn
                              read-string
                              (update-in [:password] sha-512)) ;;parsing the incoming edn
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
                   user))
                {:status 200
                 :body "true"})
              (throw (Exception. (str "User " username " exists already")))))))
  
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
