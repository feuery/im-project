
;;If client jams emacs, start the repl in terminal, although that seems to break the (println) in the server-side

(ns mese-test.web
  (:require [mese-test.auth :refer [user-authenticates!?
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
                :personal-message])

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
           (if (user-authenticates!? username password ip sessionid)
             (let [user (find-user-real username)]
               (println username " authenticated from ip " ip)
               {:status 200
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body (str "{:success true :session-id " @sessionid " :user " user "}")})
             (do
               (println "Tough luck")
               {:status 403
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body "{:success false}"})))
         (catch Exception ex
           (println ex)
           {:status 500
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false } ; Infernal server error - admin is notified"})))
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
                          (let [new-user (assoc user :user
                                                (assoc (:user user) property new-value))]
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
         ;; (println "Listing friends for " session-id ", " ip)           
         (if (session-authenticates? ip session-id)
           (do
             (let [toret (->> (people-logged-in)
                              ;(filter user-logged-in?)
                              (map logged-out)
                              (map #(dissoc % :password))
;                              (map str)
                              vec
                              str)]
               ;; (println "Returning friends: " toret)
               {:status 200
                :headers {"Content-Type" "text/plain; charset=utf-8"}
                :body toret}))
           (do
             (println "Session didn't auth")
             {:status 403
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body "{:success false }"}))
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
             :body "{:success false }"})))
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
  (jetty/run-jetty (-> #'app wrap-params) {:port (Integer. port) :join? false}))

(def server (-main 5000))
;; For interactive development:
;; (.stop server)
;; (def server (-main))
