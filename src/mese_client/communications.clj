(ns mese-client.communications
  (:require [mese-client.settings :refer [get-setting]]
            [mese-client.friends :refer [get-current-users]]
            [org.httpkit.client :as http]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]))

(defn login
  "Returns false on failure, session-id on success."
  [username password current-user-atom]
  (let [url (str (get-setting :server-url) "login/" username "/" password)]
    (println "url: " url)
    (when-let [{body :body :as result} @(http/get url)]
      (println "result: " result)
      (println "body: " body)
      (if (empty? body)
        false
        (if-let [body-map (read-string body)]
          (if (:success body-map)
            (do
              (reset! current-user-atom (:user body-map))
              (:session-id body-map))
            false))))))

(defn date? [dt]
  (instance? java.util.Date dt))
  
(defn server-time []
  (if-let [{body :body} @(http/get (str (get-setting :server-url) "timestamp/"))]
    (do
      (println "server-time body: " body)
      (if (date? (read-string body))
        (read-string body)
        (-> (time/now) tc/to-timestamp)))
    (-> (time/now) tc/to-timestamp)))
    

(defn send-msg [session-id ;; receiver
                message]
  (let [{message :message receiver :receiver} message
        options {:form-params {:message message}}]
    @(http/post (str (get-setting :server-url) "send-msg/" session-id "/receiver-handle/" receiver) options)))

(defn get-inbox [session-id]
  (let [{body :body} @(http/get (str (get-setting :server-url) "inbox/" session-id "/"))]
    (println body)
    (when-let [{success :success :as result} (read-string body)] ;;unreadable DateTime-literals fuck this up
      (if success
        (:inbox result)
        false))))
