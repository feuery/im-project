;;If client jams emacs, start the repl in terminal, although that seems to break the (println) in the server-side

(ns mese-test.web
  (:require [mese-test.auth.friend-db :refer [friend?]]
            [mese-test.auth.request-db :refer [add-friend-request!
                                               requests-of
                                               accept-request
                                               create-friend-request]]
                                               
             [mese-test.auth.user-db :refer [user-authenticates!?
                                    sessionid->userhandle
                                    session-authenticates?
                                    logout!
                           
                                    commit-user!
                                    session-belongs-to-user?
                                    logged-out
                                    username->userhandle
                                    find-user-real
                                    people-logged-in
                                    ip-to-sender-handle
                                    user-logged-in?]]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [clojure.pprint :refer [pprint]]
            [mese-test.user :refer [create-message
                                    dump-outbox!
                                    push-outbox!]]
            [mese-test.util :refer [in?]]
            [clojure.string :as s]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.route :as route]))

(def user-keys [:user-handle 
                :username
                :img-url 
                :state 
                :personal-message
                :font-preferences])

(defroutes app
  (POST "/hello-world"
        {{message "message"} :params
         ip :remote-addr}
        (format "%s said: %s" ip message))
  (GET "/get-inbox/:user/:computer-id" [user computer-id]
       (println "Hello world!")
       {:status 200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body (format "Hello %s from <h3 style=\"color: #FF0000;\">%s</h3>" user computer-id)})
  (GET "/login/:username/:password"
       {{username :username password :password} :params ip :remote-addr}
       (try
         (println "ip " ip)
         (let [sessionid (promise)]
           (println "Authing " username " with pw " password)
           (if (user-authenticates!? username password ip sessionid)
             (let [user (find-user-real username)]
               (println username " authenticated from ip " ip)
               (println "user: " user)
               {:status 200
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body (str "{:success true :session-id " @sessionid " :user " user "}")})
             (do
               (println "Tough luck")
               {:status 403
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body "{:success false}"})))
         (catch NullPointerException ex
           (println "Probably wrong pw")
           (.printStackTrace ex *out*))
         (catch Exception ex
           (println ex)
           {:status 500
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false } ; Infernal server error - admin is notified"})))
  (GET "/friend-request/:session-id/:friend-handle"
       {{session-id :session-id friend-handle :friend-handle} :params ip :remote-addr}
       (try
         (println "Beginning /friend-request/")
         (let [session-id (Long/parseLong session-id)]
           (if (session-authenticates? ip session-id)
             (let [user (sessionid->userhandle session-id)]
               (if (find-user-real friend-handle)
                 (do
                   (println "Adding request from " user " to " friend-handle)
                   (add-friend-request! friend-handle (create-friend-request user)))
                 (println "User " friend-handle " not found"))
               {:status 200
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body "{:success true}"})
             {:status 403
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body "{:success false}"}))
         (catch Exception ex
           (println ex)
           {:status 500
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false } ; Infernal server error - admin is notified"})))
  (GET "/friend-requests/:session-id"
       {{session-id :session-id} :params ip :remote-addr}
       (if (session-authenticates? ip session-id)
         (let [userhandle (sessionid->userhandle (Long/parseLong session-id))]
           (println "returning requests of " userhandle)
           {:status 200
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body (str "{:success true
:requests " (->> userhandle
                requests-of
                vec
                (filter (complement :accepted?))
                pr-str) "}")})
         {:status 403
          :headers {"Content-Type" "text/plain; charset=utf-8"}
          :body "{:success false}"}))
  (GET "/accept-request/:session-id/:requester-handle"
       {{session-id :session-id requester-handle :requester-handle} :params
        ip :remote-addr}
       (if (session-authenticates? ip session-id)
         (let [current-user (sessionid->userhandle (Long/parseLong session-id))
               requests (filter (complement :accepted?)
                                (requests-of current-user))]
           (if (in? (map :requester requests) requester-handle)
             (do
               (accept-request current-user requester-handle)
               {:status 200
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body (str "{:success true}")})
             (do
               (println "Current user: " current-user "| Not found " requester-handle " in " (map :requester requests))
               {:status 403
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body "{:success false ;1
}"})))
         {:status 403
          :headers {"Content-Type" "text/plain; charset=utf-8"}
          :body "{:success false ;2
}"}))
       
  (GET "/logout/:session-id/"
       {{session-id :session-id} :params ip :remote-addr}
       (try
         (if (session-authenticates? ip session-id)
           {:status 200
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body (str "{:success " (str (logout! session-id ip)) "}")}
           {:status 403
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false}"})         
         (catch Exception ex
           (println ex)
           {:status 500
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false } ; Infernal server error - admin is notified"})))
  (POST "/update-myself/:session-id/:user-handle/"
        {{session-id :session-id
          user-handle :user-handle
          property "property"
          new-value "new-value"} :params ip :remote-addr}
        (try
          (println "[serverside:] Swapping " user-handle "'s " property " to " new-value)
          (if (session-authenticates? ip session-id)
            (do
              (println "new-value: " (if (= property :state)
                                       (keyword (s/replace new-value #":" ""))
                                       new-value))
              (let [property (keyword (s/replace property #":" ""))
                    new-value (if (= property :state)
                                (do
                                  (println "Property was state")
                                  (keyword (s/replace new-value #":" "")))
                                (if (and (= property :img-url) (empty? new-value))
                                  "http://prong.arkku.net/MERPG_logolmio.png"
                                  new-value))]
                (if (in? user-keys property)
                  (do
                    (println "We're in!")
                    (let [user (find-user-real user-handle :with-sessions? true)]
                      (println "usr: " user)
                      
                      (if (session-belongs-to-user? user session-id)
                        (do
                          (println "Session belongs to us!")
                          (println "usr: " user)
                          (let [new-user (assoc user property new-value)]
                            (println "Committing user " new-user)

                            (commit-user! user-handle new-user)
                            {:status 200
                             :headers {"Content-Type" "text/plain; charset=utf-8"}
                             :body  "{:success true}"}))
                        (println "session is broken!"))))
                  (do
                    (println "usrkeys: " user-keys " | property: " property "(" (class property) ")")
                    {:status 403
                     :headers {"Content-Type" "text/plain; charset=utf-8"}
                     :body "{:success false ;third
}"}))))
            {:status 403
             :headers {"Content-Type" "text/plain; charset=utf-8"}
             :body "{:success false}"})
          
          (catch Exception ex
           (println ex)
           {:status 500
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false } ; Infernal server error - admin is notified"})))
  (GET "/list-friends/:session-id"
       {{session-id :session-id} :params
        ip :remote-addr}
       (try
         (let [session-id (Long/parseLong session-id)]
           (println "Listing friends for " session-id ", " ip)           
           (if (session-authenticates? ip session-id)
             (do
               (println "session authed")
               (let [current-user (sessionid->userhandle session-id)
                     _ (println "current-user: " current-user " for sessid: " session-id " (" (class session-id) ")")
                     toret (->> (people-logged-in)
                                (filter #(friend? current-user (:user-handle %)))
                                (map logged-out)
                                (map #(dissoc % :password :sessions))
                                vec
                                str)]
                 (println "Returning friends: " toret)
                 {:status 200
                  :headers {"Content-Type" "text/plain; charset=utf-8"}
                  :body toret}))
             (do
               (println "Session didn't auth")
               {:status 403
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body "{:success false }"})))
         (catch Exception ex
           (clojure.pprint/pprint ex)
           (println "Poikkeus list-friendsissa")
           {:status 500
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false } ; Infernal server error - admin is notified"}))) ;;If you want to exclude yourself, please do it in the client-side
  (POST "/send-msg/:session-id/receiver-handle/:receiver-handle"
        {{receiver-handle :receiver-handle
          session-id :session-id
          message "message"} :params
          ip :remote-addr}
        (println "Tä? msg: " message ", receiver " receiver-handle)
        (try
          ; (println session-id "/" (sessionid->userhandle session-id) " sending to " receiver-handle)
          (if (and (session-authenticates? ip session-id)
                   (friend? receiver-handle (sessionid->userhandle (Long/parseLong session-id))))
            (let [result (try
                           (-> (ip-to-sender-handle ip)
                               (create-message message receiver-handle)
                               push-outbox!)
                           (catch NullPointerException ex
                             (println "NPE@result-gathering")
                             (throw ex)))]
              (println "session authed")
              (println "result " result)
              {:status 200
               :headers {"Content-Type" "text/plain; charset=utf-8"}
               :body "{:success true}"})
            (do
              (println "session not authed from ip " ip)
              {:status 403
               :headers {"Content-Type" "text/plain; charset=utf-8"}
               :body "{:success false }"}))
          (catch Exception ex
            (println ex)
            {:status 500
             :headers {"Content-Type" "text/plain; charset=utf-8"}
             :body "{:success false } ; Infernal server error - admin is notified"})))
  (GET "/timestamp/"
       []
       {:status 200
        :headers {"Content-Type" "text/plain; charset=utf-8"}
        :body (pr-str (-> (time/now) tc/to-timestamp))})
        
  (GET "/inbox/:session-id/"
       {{session-id :session-id} :params
        ip :remote-addr}
       (println "IP: " ip "; session-id " session-id)
       (try
         (let [receiver (ip-to-sender-handle ip)]
           (if (session-authenticates? ip session-id)
             {:status 200
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body (str "{:success true
:inbox " (dump-outbox! ip session-id receiver) "}")}
             (do
               (println "Auth fail")
               {:status 403
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body "{:success false }"})))
         (catch Exception ex
           (println "routes blew up")
           (println ex))))
  (ANY "*" []
       "Hello world! I'm online!"))
  
         

(defn -main [port]
  (jetty/run-jetty (-> #'app wrap-params) {:port (Integer. port) :join? false})
  (mese-test.db/init)
  (mese-test.auth.user-db/init)
  (mese-test.auth.friend-db/init)
  (mese-test.auth.request-db/init))

; (def server (-main 5000))
;; For interactive development:
;; (.stop server)
;; (def server (-main))
