(ns improject.middleware
  (:require [ring.middleware.defaults :refer [api-defaults site-defaults wrap-defaults]]))

(defn wrap-middleware [handler]
  (wrap-defaults handler api-defaults))
