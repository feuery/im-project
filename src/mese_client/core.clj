(ns mese-client.core
  (:require [seesaw.core :refer :all]
            [mese-client.ui.login-form :refer [get-credentsials]]
            [mese-client.ui.main-form :refer [show-mainform]]
            [mese-test.util :refer [map-to-values]]
            [mese-client.settings :refer [server-url]]
            [mese-client.friends :refer [get-current-users]]
            [org.httpkit.client :as http]))

(native!)

(defn login
  "Returns false on failure, session-id on success."
  [username password]
  (let [url (str server-url "login/" username "/" password)]
    (println "url: " url)
    (when-let [{body :body :as result} @(http/get url)]
      (println "result: " result)
      (println "body: " body)
      (if (empty? body)
        false
        (if-let [body-map (read-string body)]
          (if (:success body-map)
            (:session-id body-map)
            false))))))
    

(defn send-msg [session-id receiver message]
  (let [options {:form-params {:message message}}]
    @(http/post (str server-url "send-msg/" session-id "/receiver-handle/" receiver) options)))

(defn get-inbox [session-id]
  (let [{body :body} @(http/get (str server-url "inbox/" session-id "/"))]
    (println body)
    (when-let [{success :success :as result} (read-string body)] ;;unreadable DateTime-literals fuck this up
      (if success
        (:inbox result)
        false))))

;(defn -main [& argh]
(let [login-map (get-credentsials)]
  (comment     {:username username
     :password password
     :window-state window-state})
  
  (while (= @(:window-state login-map) :open)
    (Thread/sleep 5))

  (when (= @(:window-state login-map) :ready)
    (let [{username :username password :password} (map-to-values deref login-map)
          session-id (login username password)]
      (if session-id
        (do
          (println "sessid: " session-id)
          (println "Showing mainform")
          (show-mainform session-id))
        (str "sessid-fail: " session-id)))))
