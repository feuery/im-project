(ns mese-test.user
  (:require [mese-test.util :refer [in?]]
            
;            [mese-test.auth :refer [ip-to-sender-handle]]
            [clojure.pprint :refer :all]
            [mese-client.communications :refer [server-time]]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]))

(alter-var-root #'*out* (constantly *out*))

(def running-in-server? false) ;;TODO Again, with the settings..

(def possible-states [:online
                      :busy
                      :away
                      :returning
                      :lunch
                      :fake-offline
                      :real-offline])

(defn create-user
  "This creates the user-molds, that can be users checked against" 
  [user-handle username img-url state pw-hash]
  {:pre [(in? possible-states state)]}
  {:user-handle user-handle 
   :username username
   :password pw-hash
   :img-url img-url 
   :state state 
   :personal-message "This is a personal message"
   :font-preferences {:bold false
                       :italic false
                       :color "#000000"
                       :font-name "arial"}})

(def inboxes (ref {}))
(def outboxes (ref {}))

(doseq [r [inboxes outboxes]]
  (add-watch r :state-watcher (fn [_ _ _ new-state]
                 (println (if (= r inboxes)
                            "inboxes"
                            "outboxes") " changed: ")
                 (pprint new-state))))

(defn create-message [sender-id msg receiver-id]
  {:pre [(or (in? (keys @inboxes) receiver-id)
             (not running-in-server?))]}
  {:sender sender-id :message msg :receiver receiver-id
   :time (-> (time/now) tc/to-timestamp)
   :sent-to-sessions []})

(defn create-message-client [& params]
  (-> (apply create-message params)
      (assoc :time (server-time))))

(defn dump-outbox! [ip session-id receiver-name]
  (dosync
   (let [receiver receiver-name
         receivers-inbox (get @inboxes receiver)
         messages-to-send (->> receivers-inbox
                               (filter #(not (in? (:sent-to-sessions %) session-id)))
                               (sort-by :time)
                               vec)
         receivers-new-inbox (map #(if (in? messages-to-send %)
                                     (assoc % :sent-to-sessions
                                            (conj (:sent-to-sessions %) session-id))
                                     %) receivers-inbox)]
;     (println "receiver: " receiver)
;     (println "The old inbox ")
;     (pprint receivers-inbox)
;     (println "the new inbox ")
;     (pprint receivers-new-inbox)
     (alter inboxes assoc receiver receivers-new-inbox)

     messages-to-send)))
     
    
    


(defn get-inbox
  "Returns nil if the inbox doesn't exist"
  [user-handle]
  (get @inboxes user-handle))

(defn get-inbox!
  "Creates the inbox if it doesn't exist"
  [user-handle]
  (let [inbox-exists (-> @inboxes
                         keys
                         (in? user-handle))]
    (when-not inbox-exists
      (dosync
       (alter inboxes assoc user-handle [])))
    (get @inboxes user-handle)))

(defn get-outbox
  "Returns nil if the outbox doesn't exist"
  [user-handle]
  (get @outboxes user-handle))

(defn msg-dispatcher [user-handle]
  (loop []
    (while (empty? (get @outboxes user-handle))
      (Thread/sleep 400))
    (println "Doing processing outbox of " user-handle)
    (let [[message & rest] (get @outboxes user-handle)
          rest (if (nil? rest) [] rest)
          inbox (get-inbox! (:receiver message))]
      (println "Conjing " message " to " inbox)
      (dosync
       (alter outboxes assoc user-handle rest)
       (alter inboxes assoc (:receiver message) (conj inbox message))))
    (recur)))

(defn get-outbox!
  "Creates the outbox if it doesn't exist"
  [user-handle]
  (let [outbox-exists (-> @outboxes
                         keys
                         (in? user-handle))]
    (when-not outbox-exists
      (dosync
       (alter outboxes assoc user-handle []))
      (doto (Thread. (partial msg-dispatcher user-handle))
        .start)
      (println "outbox-thread started for " user-handle))
    (get @outboxes user-handle)))

(defmacro get-ob [[ob get-param] & forms]
  `(if-let [~ob (get-outbox ~get-param)]
     (dosync
      ~@forms)
     (throw (Exception. (str "Getting outbox " '(get-outbox ~get-param) " failed")))))

(defn push-outbox! [message]
  (get-ob [ob (:sender message)]
          (alter outboxes assoc (:sender message) (sort-by :time (conj ob message)))))

(defn pop-outbox!
  "Returns the first element out of the user's outbox queue, returns it and updates the outbox."
  [user]
  (get-ob [ob (:user-handle user)]
          (let [toret (first ob)]
            (alter outboxes assoc (:user-handle user) (rest ob))
            toret)))

(defn push-inbox! [message]
  (if-let [ib (get-inbox (:receiver message))]
          (alter inboxes assoc (:receiver message) (->> (conj ib message)
                                                        (sort-by :time)
                                                        reverse))))                
(dosync
   (ref-set inboxes {})
   (ref-set outboxes {}))
