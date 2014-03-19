(ns mese-client.core
  (:require [seesaw.core :refer :all]
            [mese-client.ui.login-form :refer [get-credentsials]]
            [mese-client.ui.main-form :refer [show-mainform]]
            [mese-client.friends :refer [update-myself
                                         update-font]]
            [mese-client.communications :refer [login]]
            [mese-test.util :refer [map-to-values
                                    in?]]))

(native!)

;;TODO Remove from published versions...
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.gtk.GTKLookAndFeel")

(def current-user (atom nil))

(defn diff [map1 map2]
  (->> (keys map1)
       (filter (partial in? (keys map2)))
       (filter #(not (= (get map1 %) (get map2 %))))))                                   

;(defn -main [& argh]
(let [login-map (get-credentsials)]
  (comment     {:username username
     :password password
     :window-state window-state})
  
  (while (= @(:window-state login-map) :open)
    (Thread/sleep 5))

  
  (when (= @(:window-state login-map) :ready)
    (let [{username :username
           password :password} (map-to-values deref login-map)
          _ (println "salis: " password)
          session-id (login username password current-user)]
      (if session-id
        (do
          (println "sessid: " session-id)
          (println "Showing mainform")
          (add-watch current-user :watcher  (fn [_ _ old new]
                                              (println "New of current-user: " new)
                                              (let [diff-keys (diff old new)]
                                                (doseq [key diff-keys]
                                                  (when-not (= key :font-preferences)
                                                    (println "Updating " key " with val " (get new key))
                                                    (update-myself session-id (:user-handle @current-user) key (get new key)))
                                                  (when (= key :font-preferences)
                                                    (let [new-prefs (get new key)]
                                                      (update-font session-id (:user-handle @current-user) ;;There has to be a way to make this less-fugly
                                                                   :bold? (get new-prefs :bold?)
                                                                   :italic? (get new-prefs :italic?)
                                                                   :underline? (get new-prefs :underline?)
                                                                   :color (get new-prefs :color)
                                                                   :font-name (get new-prefs :font-name))))))))

          (show-mainform session-id current-user))
        (str "sessid-fail: " session-id)))))
