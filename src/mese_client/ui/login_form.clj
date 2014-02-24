(ns mese-client.ui.login-form
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [mese-test.util :refer [in?]]))

(def window-states [:open :canceled :ready])

(defn get-credentsials
  "When value of atom of key :window-state is :ready, other atoms contain the information asked from the user. While the value is :open, the form is open, and when it is :canceled, user has canceled the login and the app should kill itself"
  []
  (let [username (atom "")
        usrnamefield (text)      
        passwordfield (password)
        password (atom "")
        
        window-state (atom :open :validator #(in? window-states %))
        
        f (frame :title "YOOL-IM (working title)"
                 :width 640
                 :height 480
                 :visible? true
                 :on-close :dispose
                 :listen [:window-closed (fn [_]
                                           (if (= @window-state :open)
                                             (reset! window-state :canceled)))])
        login-event-fn (fn [_]
                         (reset! window-state :ready)
                         (-> f hide! dispose!))
        login-btn (button :text "Log in" :listen [:action-performed login-event-fn])]
    (config! f :content (vertical-panel
                         :items [(horizontal-panel :items [(label :text "Username") usrnamefield])
                                 (horizontal-panel :items [(label :text "Password") passwordfield])
                                 (horizontal-panel :items
                                                   [login-btn
                                                    (button :text "Close" :listen [:action-performed
                                                                                   (fn [_]
                                                                                     (reset! window-state :canceled)
                                                                                     (-> f hide! dispose!))])])]))
    (listen passwordfield :key-released (fn [e]
                              (let [keychar (.getKeyChar e)]
                                (when (= keychar \newline)
                                  (login-event-fn nil)))))
    (b/bind usrnamefield username)
    (b/bind passwordfield password)

    (comment    (doseq [a [username password window-state]]
                  (add-watch a :jee (fn [_ _ _ new-val]
                                      (println "new-val is " new-val)))))
    
    (-> f pack!)

    {:username username
     :password password
     :window-state window-state}))
