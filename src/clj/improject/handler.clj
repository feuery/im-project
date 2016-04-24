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
            [improject.schemas :refer [user-schema]]
            [improject.serialization :refer [save-user!]]
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

(defmacro with-validation [[obj form schema] & forms]
  `(try
     (let [~obj (schemas/validate ~schema ~form)]
       ~@forms)
     (catch Exception ex#
       (pprint ex#)
       {:status 500
        :body "Infernal Server Error"})))

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

  (POST "/register-user" {{edn :edn} :params}        
        (with-validation [{username :username
                           :as user} ;;destructuring
                          (-> edn
                              read-string
                              (update-in [:password] sha-512)) ;;parsing the incoming edn
                          user-schema ;;schema
                          ]
          (let [interesting-users (k/select users
                                            (k/where {:username username}))]
            (if (empty? interesting-users)
              (do
                (save-user! (assoc user :can_login true :admin true))
                {:status 200
                 :body "true"})
              (throw (Exception. (str "User " username " exists already")))))))
  
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
