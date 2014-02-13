(ns mese-test.web
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.route :as route]))

(defroutes app
  (GET "/get-inbox/:user/:computer-id" [user computer-id]
       (format "Hello %s from <h3 style=\"color: #FF0000;\">%s</h3>" user computer-id)))

(defn -main [port]
  (jetty/run-jetty #'app {:port (Integer. port) :join? false}))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
