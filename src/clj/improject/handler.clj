(ns improject.handler
  (:require [compojure.core :refer [POST GET defroutes]]
            [figwheel-sidecar.repl-api :as s]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [improject.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [korma.core :refer [select]]
            [clojure.edn :refer [read-string]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]

            [improject.db :refer [users]]))

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

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  (GET "/no-users" []
       (let [users (select users)]
         {:status 200
          :headers {"Content-Type" "text/plain"}
          :body (if (empty? users)
                  "true"
                  "false")}))

  (POST "/register-user"
        {{edn :edn} :params}
        (let [obj (read-string edn)]
          (html5
           [:body
            [:p "Got back " (prn-str obj)]])))
  
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
