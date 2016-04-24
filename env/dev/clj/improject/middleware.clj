(ns improject.middleware
  (:require [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.session-timeout :refer [wrap-idle-session-timeout]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults api-defaults)
      wrap-exceptions
      wrap-reload
      (wrap-session {:store (memory-store)})
      (wrap-idle-session-timeout {:timeout (* 30 60)
                                  :timeout-response
                                  {:status 404
                                   :body "Timeout"}})))
