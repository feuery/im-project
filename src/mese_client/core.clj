(ns mese-client.core
  (:require [seesaw.core :refer :all]
            [mese-client.ui.login-form :refer [get-credentsials]]
            [mese-client.ui.main-form :refer [show-mainform]]
            [mese-client.communications :refer [login]]
            [mese-test.util :refer [map-to-values]]))

(native!)

(def current-user (atom nil))

;(defn -main [& argh]
(let [login-map (get-credentsials)]
  (comment     {:username username
     :password password
     :window-state window-state})
  
  (while (= @(:window-state login-map) :open)
    (Thread/sleep 5))

  (when (= @(:window-state login-map) :ready)
    (let [{username :username password :password} (map-to-values deref login-map)
          session-id (login username password current-user)]
      (if session-id
        (do
          (println "sessid: " session-id)
          (println "Showing mainform")
          (show-mainform session-id current-user))
        (str "sessid-fail: " session-id)))))
