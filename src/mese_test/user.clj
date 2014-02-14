(ns mese-test.user
  (:require [mese-test.util :refer [in?]]
            [clj-time.core :as time]
            [clojure.pprint :refer :all]
            [clj-time.coerce :as tc]))

(alter-var-root #'*out* (constantly *out*))

(def possible-states [:online
                      :busy
                      :away
                      :returning
                      :lunch
                      :fake-offline
                      :real-offline])

(defn create-user [user-handle username img-url state]
  {:pre [(in? possible-states state)]}
  {:user-handle user-handle 
   :username username 
   :img-url img-url 
   :state state 
   :personal-message "This is a personal message"})

(def inboxes (ref {}))
(def outboxes (ref {}))
(def users (atom []))

(defn create-user! [& args]
  (let [toret (apply create-user args)]
    (swap! users conj toret)
    toret))

(doseq [r [inboxes outboxes]]
  (add-watch r :state-watcher (fn [_ _ _ new-state]
                 (println (if (= r inboxes)
                            "inboxes"
                            "outboxes") " changed: ")
                 (pprint new-state))))
                 

(def feuer (create-user "feuer" "Feuer" "http://3.bp.blogspot.com/_z3wgxCQrDJY/S6CgYhXSkyI/AAAAAAAAAAg/0Vv0ffa871g/S220/imagex100x100.jpeg" :online))

(defn create-message [sender-id msg receiver-id]
  {:pre [(in? (keys @inboxes) receiver-id)]}
  {:sender sender-id :message msg :receiver receiver-id
   :time (time/now)})


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

(defn get-outbox!
  "Creates the outbox if it doesn't exist"
  [user-handle]
  (let [outbox-exists (-> @outboxes
                         keys
                         (in? user-handle))]
    (when-not outbox-exists
      (dosync
       (alter outboxes assoc user-handle [])))
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

(defmacro while-let [[obj expr] & forms]
  `(loop [~obj ~expr]
    ~@forms
    (if-let [next# ~expr]
      (recur next#))))

(defn empty-outboxes!
  "Moves every message of every outbox to their recipient's inbox"
  [users-atom]
  (try
    (loop []
      (println "Began emptying outboxes...")
      (doseq [user @users-atom]
        (dosync
         (println "User@empty-outboxes: " user)

         ;;Tämä räjähtää tietysti, koska user on string eikä usermappi
         (while-let [msg (pop-outbox! user)]
                    (push-inbox! msg))))
      (println "Emptyed everything! Recurring!")
      (recur))
    (catch Exception ex
      (println "Caught thing: " ex)
      (.printStackTrace ex) )))
                
(do
  (get-outbox! (:user-handle feuer))
  (get-inbox! (:user-handle feuer)))
