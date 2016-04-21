(ns improject.handler
  (:require [compojure.core :refer [GET defroutes]]
            [figwheel-sidecar.repl-api :as s]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [improject.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]))

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

;; (when (env :roland false)
;;   ;; (def figwheel-config
;;   ;;   {:figwheel-options {} ;; <-- figwheel server config goes here 
;;   ;;    :build-ids ["dev"]   ;; <-- a vector of build ids to start autobuilding
;;   ;;    :all-builds          ;; <-- supply your build configs here
;;   ;;    [{:id "dev"
;;   ;;      :figwheel true
;;   ;;      :source-paths ["cljs-src"]
;;   ;;      :compiler {:main "repler.core"
;;   ;;                 :asset-path "js/out"
;;   ;;                 :output-to "resources/public/js/repler.js"
;;   ;;                 :output-dir "resources/public/js/out" }}]})
;;   (s/start-figwheel! ;; figwheel-config
;;                      )
;;   )

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
