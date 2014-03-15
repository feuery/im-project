(ns mese-client.ui.discussion
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [seesaw.make-widget :refer [MakeWidget]]
            [clojure.string :as s]
            [seesaw.border :as border]
            [mese-test.util :refer [seq-in-seq? in?]]
            [mese-test.user :refer [create-message]]
            [merpg.2D.core :as c]
            [mese-client.communications :refer :all]
            [mese-client.friends :refer [state-to-color]])
    (:import [java.net URL]
           [java.awt Dimension]))

(comment
            +-------------------------------------------------------+
            |   Your friend's name               (State)            |
            |   Friend's personal msg                               |
            +-------------------------------------------------------+
            | Your fri- |                                           |
            | end's img |                                           |
            +-----------+                                           |
            |           |                                           |
            |           |                                           |
            |           |       Discussion                          |
            |           |                                           |
            |           |                                           |
            |           +-------------------------------------------+
            |           |                                           |
            +-----------+                            -----------    |
            |           |                           (    SEND   )   |
            |           |                            -----------    |
            | Your pers-|        Your msg                           |
            | onal img  |                                           |
            |           |                                           |
            +-----------+-------------------------------------------+)

(def user-keys [:user-handle :username :img-url :state :personal-message])


(defn make-imgview [usratom id]
  (-> @usratom
      :img-url
      (URL.)
      make-widget
      (config! :background (state-to-color (:state @usratom))
               :size [100 :by 120]
               :id id)))

(defn discussion-form [current-user-atom session-id friend]
  {:pre [(seq-in-seq? user-keys (keys @friend))]}
  (try
    (println "Doing discussion")
    (let [discussion (atom [])
          current-message (atom "jeeee")
          form (frame
                :width 640
                :height 480 ;;TODO Setup a settings-repository for these, and update it on-resize
                :visible? true
                :on-close :dispose
                :content
                (vertical-panel
                  :items
                  [(label :text (str (:username @friend) "    -    (" (-> @friend :state str (s/replace #":" "")) ")")
                          :font "ARIAL-BOLD-18"
                          :id :friend-name)
                   (label :text (:personal-message @friend)
                          :font "ARIAL-15"
                          :id :friend-persmes)
                   (horizontal-panel
                    :items
                    [(border-panel :north (-> @friend
                                              :img-url
                                              (URL.)
                                              make-widget
                                              (config! :background (state-to-color (:state @friend))
                                                       :size [100 :by 120]
                                                       :id :friend-image))
                                   :south (make-imgview current-user-atom :own-image))
                     (top-bottom-split (scrollable (text :multi-line? true :editable? false
                                                         :wrap-lines? true :id :discussion)
                                                   :minimum-size [200 :by 600])
                                       (border-panel :center (text :multi-line? true
                                                                   :wrap-lines? true
                                                                   :id :msg)
                                                     :east (button :text "SEND" :listen [:action-performed
                                                                                         (fn [_]
                                                                                           (alert "Not implemented"))]))
                                       :divider-location 2/3)])]))]
      (add-watch discussion :msg-sender (fn [_ _ _ [{sender :sender
                                                     message :message :as new-msg} & _]]
;                                          (println sender " = " 
                                          (when (= sender (:user-handle @current-user-atom))
                                            (println "Really sending " message " from " sender)
                                            (send-msg session-id new-msg))))
                                            
      
      (listen (select form [:#msg])
              :key-released
              (fn [e]
                (println (if (= (.getKeyChar e) \newline) "\\newline" (.getKeyChar e)))
                (if (and (= (.getKeyChar e) \newline)
                         (not (.isAltDown e)))
                  (do
                    ;; (println "Sending " (text e) " to " (:user-handle @friend))
                    ;; (send-msg session-id
                    ;;           (:user-handle @friend)
                    ;;           (text e))
                    ;; (let [disc (select form [:#discussion])]
                    ;;   (text! disc (str (text disc) "\n" (:username @current-user-atom) " says:\n"
                    ;;                    (text e))))
                    (println "Sending msg.... " (text e))
                    (swap! discussion #(vec (cons (create-message (:user-handle @current-user-atom)
                                                             (text e)
                                                             (:user-handle @friend)) %)))
                                                             
                    (text! e ""))
                  (if (= (.getKeyChar e) \newline)
                    (text! e (str (text e) \newline))))))

      (b/bind current-message
               (select form [:#msg]))
      (b/bind (select form [:#msg])
              current-message)

      (add-watch current-message :msg-thingie (fn [_ _ _ new]
                                                (println "Currently the text of the msg: " new)))
      
      (b/bind discussion
              (b/transform (fn [discussion]
                             (->>
                              discussion
                              (sort-by :time)
                              (map (fn [message]
                                     (format (str "("  (:time message) ") %s says:\n%s")
                                             (:sender message)
                                             (:message message))))
                              (reduce str))))
              (b/property (select form [:#discussion]) :text))
      
      (b/bind friend
              (b/transform #(str (:username %) "    -    (" (s/replace (:state %) #":" "") ")"))
              (b/property (select form [:#friend-name]) :text))
      (b/bind friend
              (b/transform :personal-message)
              (b/property (select form [:#friend-persmes]) :text))
      (b/bind friend
              (b/transform #(try
                              ((comp state-to-color :state) %)
                              (catch Exception ex ;;Catch the typos in the state...
                                (println ex))))
              (b/property (select form [:#friend-image]) :background))

      (b/bind current-user-atom
              (b/transform #(try
                              ((comp state-to-color :state) %)
                              (catch Exception ex ;;Catch the typos in the state...
                                (println ex))))
              (b/property (select form [:#own-image]) :background))
      (b/bind current-user-atom
              (b/transform #(-> % :img-url URL.))
              (b/property (select form [:#own-image]) :icon))
      
      
      {:window form :discussion discussion})
    (catch Exception ex
      (println ex)
      (throw ex))))

(comment #(try
                              (if (instance? clojure.lang.Atom %)
                                (-> % deref :username)
                                (:username %))
                              (catch Exception ex
                                (println "Joku tekee tyhmyyksiä transformissa discussionissa")
                                (throw ex))))
