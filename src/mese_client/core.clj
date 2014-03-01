(ns mese-client.core
  (:require [seesaw.core :refer :all]
            [mese-client.ui.login-form :refer [get-credentsials]]
            [mese-test.util :refer [map-to-values]]
            [org.httpkit.client :as http]))

(native!)

(def server-url "http://localhost:5000/")

(comment
  (let [options {:form-params {:name "http-kit" :features ["async" "client" "server"]}}
        {:keys [status error]} @(http/post "http://host.com/path1" options)]))

(defn login
  "Returns false on failure, session-id on success."
  [username password]
  (if-let [{body :body} @(http/get (str server-url "login/" username "/" password))]
    (if-let [body-map (read-string body)]
    (if (:success body-map)
      (:session-id body-map)
      false))))
    

(defn send-msg [session-id receiver message]
  (let [options {:form-params {:message message}}]
    @(http/post (str server-url "send-msg/" session-id "/receiver-handle/" receiver) options)))

;(defn -main [& argh]
(let [login-map (get-credentsials)]
  (comment     {:username username
     :password password
     :window-state window-state})
  
  (while (= @(:window-state login-map) :open)
    (Thread/sleep 5))

  (when (= @(:window-state login-map) :ready)
    (let [{username :username password :password} (map-to-values login-map deref)]
      (alert (str "Hello, " username)))))
