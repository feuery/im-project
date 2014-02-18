(ns mese-test.web
  (:require [mese-test.auth :refer [user-authenticates!?
                                    session-authenticates?
                                    people-logged-in]]
            [clojure.string :as s]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.route :as route]))


(defroutes app
  (GET "/get-inbox/:user/:computer-id" [user computer-id ip]
       (format "Hello %s from <h3 style=\"color: #FF0000;\">%s</h3>" user computer-id))
  (GET "/login/:username/:password"
       {{username :username password :password} :params ip :remote-addr}
       (let [session-id (promise)]
         (if (user-authenticates!? username password ip session-id)
           (do
             (println username " authenticated with sessionid " @session-id " from ip " ip)
             {:status 200
              :headers {"Content-Type" "text/plain; charset=utf-8"}
              :body (format "{:success true :session-id %}" @session-id)})
           {:status 403
            :headers {"Content-Type" "text/plain; charset=utf-8"}
            :body "{:success false :session-id -500}"})))
  (GET "/list-friends/:session-id"
       {{sessionid :session-id} :params ip :remote-addr}
       (let [vectorify #(str "[" % "]")]
       (if (session-authenticates? sessionid ip)
         {:status 200
          :headers {"Content-Type" "text/plain; charset=utf-8"}
          :body (->> (people-logged-in)
                     (map #(str "\"" % "\""))
                     s/join
                     vectorify)}
         {:status 403
          :headers {"Content-Type" "text/plain; charset=utf-8"}
          :body "{:success false }"}))))

(defn -main [port]
  (jetty/run-jetty #'app {:port (Integer. port) :join? false}))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
