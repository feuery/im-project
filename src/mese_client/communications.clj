(ns mese-client.communications
  (:require [mese-client.settings :refer [server-url]]
            [mese-client.friends :refer [get-current-users]]
            [org.httpkit.client :as http]))
(defn login
  "Returns false on failure, session-id on success."
  [username password current-user-atom]
  (let [url (str server-url "login/" username "/" password)]
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
