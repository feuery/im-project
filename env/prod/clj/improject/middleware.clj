(ns improject.middleware
  (:require [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.defaults :refer [api-defaults site-defaults wrap-defaults]]
            [ring.middleware.session-timeout :refer [wrap-idle-session-timeout]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults api-defaults)
      (wrap-session {:store (memory-store)})
      (wrap-idle-session-timeout {:timeout (* 30 60)
                                  :timeout-response
                                  {:status 404
                                   :body "Timeout"}})))
