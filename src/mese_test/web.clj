(ns mese-test.web
  (:require [mese-test.auth :refer [user-authenticates!?
                                    session-authenticates?
                                    people-logged-in
                                    ip-to-sender-handle]]
            [mese-test.user :refer [create-message
                                    push-outbox!]]
            [clojure.string :as s]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.route :as route]))

;;Session-authenticatesissa kusee tyypit
(defroutes app
  (GET "/get-inbox/:user/:computer-id" [user computer-id]
       (println "Hello world!")
       {:status 200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body (format "Hello %s from <h3 style=\"color: #FF0000;\">%s</h3>" user computer-id)})
  (GET "/login/:username/:password"
       {{username :username password :password} :params ip :remote-addr}
       (let [sessionid (promise)]
         (if (user-authenticates!? username password ip sessionid)
           (do
             (println username " authenticated from ip " ip)
             {:status 200
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body (str "{:success true :session-id " @sessionid "}")})
           (do
             (println "Tough luck")
             {:status 403
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body "{:success false}"}))))
  (GET "/list-friends/:session-id"
       {{session-id :session-id} :params
        ip :remote-addr}
       (println "Listing friends for " session-id ", " ip)
       (let [vectorify #(str "[" % "]")]
         
         (if (session-authenticates? ip session-id)
           (do
             (println "Session did auth")
             {:status 200
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body (->> (people-logged-in)
                         (map #(str "\"" % "\""))
                         (s/join " ")
                         vectorify)})
           (do
             (println "Session didn't auth")
             {:status 403
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body "{:success false }"})))) ;;If you want to exclude yourself, please do it in the client-side
  (POST "/send-msg/:session-id/receiver-handle/:receiver-handle"
        {{receiver-handle :receiver-handle
          session-id :session-id
          message "message"} :params
          ip :remote-addr}
        (if (session-authenticates? ip session-id)
          (let [result (-> (ip-to-sender-handle ip)
                           (create-message message receiver-handle)
                           push-outbox!)]
            (println "session authed")
            (println "result " result)
            {:status 200
             :headers {"Content-Type" "text/plain; charset=utf-8"}
             :body "{:success true}"})
          (do
            (println "session not authed from ip " ip)
            {:status 403
             :headers {"Content-Type" "text/plain; charset=utf-8"}
             :body "{:success false }"}))))
(comment  (GET "/inbox/:session-id/"
       {{session-id :session-id} :params
        ip :remote-addr}
       (if (session-authenticates? ip session-id))))
         ;;Merkkaa kaikkiin inboxin viesteihin että tähän session-id:hen ne on jo lähetetty
         ;;Palauta ne responsena...
         ;;Miten ref-transaktioon varmistaa että ne oikeasti vastaanotetaan?
         ;;Ei varmaan mitenkään.... :P
         

(defn -main [port]
  (jetty/run-jetty (-> #'app wrap-params) {:port (Integer. port) :join? false}))

(def server (-main 5000))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
