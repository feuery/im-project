(ns mese-test.auth
  (:require [mese-test.user :refer [create-user
                                    get-outbox!
                                    get-inbox!]]
            [clojure.pprint :refer :all]
            [mese-test.util :refer [in?
                                    to-number]]))

(def Users (atom []))

(defn sha-512 [data]
  (let [md (. java.security.MessageDigest getInstance "sha-512")]
    (. md update (.getBytes data))
    (let [bytes (. md digest)]
      (reduce #(str %1 (format "%02x" %2)) "" bytes))))

(def feuer (create-user "feuer" "Feuer" "http://3.bp.blogspot.com/_z3wgxCQrDJY/S6CgYhXSkyI/AAAAAAAAAAg/0Vv0ffa871g/S220/imagex100x100.jpeg" :online (sha-512 "testisalasana")))
(def new-recipient (create-user "new" "moimaailma" "http://prong.arkku.net/MERPG_logolmio.png" :online (sha-512 "testisalasana")))

(do
  (get-outbox! (:user-handle feuer))
  (get-inbox! (:user-handle feuer))
  (get-outbox! (:user-handle new-recipient))
  (get-inbox! (:user-handle new-recipient)))

(def user-db [feuer new-recipient])

(defn find-user [user-db user-handle]
  (or
   (first (filter #(= (:user-handle %) user-handle) user-db))
   false))

(defn user-authenticates? [user-db username naked-password]
  (let [password (sha-512 naked-password)
        user (find-user user-db username)]
    (= password (:password user))))

(def used-sessionids (atom []))

(defn get-sessionid! []
  (loop [sessid (rand-int Integer/MAX_VALUE)]
    (if (in? @used-sessionids sessid)
      (recur (rand-int Integer/MAX_VALUE))
      (do
        (swap! used-sessionids conj sessid)
        sessid))))

(defn user-authenticates!?
  "If this returns true, the user is marked logged in"
  [username naked-password ip sessionid-promise]
    (if (user-authenticates? user-db username naked-password)
      (let [user (find-user user-db username)]
        (if-not (in? (map :user @Users) user)
          (let [session-id (get-sessionid!)]
            (println username " signed in for the first time") 
            (swap! Users conj {:user user :sessions (atom {ip {:last-call (System/currentTimeMillis)
                                                               :session-id session-id}})})
            (println "delivering " session-id)
            (deliver sessionid-promise session-id))
          (let [{sessions :sessions} (->> @Users
                                          (filter #(= (-> %
                                                          :user
                                                          :user-handle) username))
                                          first)
                old-val (get @sessions ip)
                new-val (assoc old-val :last-call (System/currentTimeMillis))]
            (println username " signed in again") 
            (let [toret (swap! sessions assoc ip new-val)]
              (println "the new sessions: " @sessions)
              (deliver sessionid-promise (:session-id new-val)))))
        (println "Returning true...")
        true)
      (do
        (println "Returning false")
        false)))

(defn session-authenticates? [ip session-id]
  (try
    (let [sessions (->> @Users
                        (map (comp deref :sessions))
                        (reduce merge))
          relevant-atom (->> @Users
                             (map :sessions)
                             (filter #(contains? @% ip))
                             first) ;; Shame on you if signing twice from the same ip...
          old-val (get @relevant-atom ip)
          new-val (assoc old-val :last-call (System/currentTimeMillis))
          five-min-in-ms (* 5 60 1000)]      
      
      (if (and (contains? sessions ip)
               (< (- (System/currentTimeMillis)
                     (-> sessions (get ip) :last-call))
                  five-min-in-ms))
        (try
          (if (= (to-number session-id) (to-number (:session-id (get sessions ip))))
            (do
              (swap! relevant-atom assoc ip new-val)
              true)
            false)
          (catch ClassCastException ex
            (println "session-id " session-id " (" (class session-id) ") tai " (:session-id (get sessions ip)) " ( " (class (:session-id (get sessions ip))) ") kusee")
            (throw ex)))
        (do
          (println "WTF?")
          false)))
    (catch Exception ex
      (println ex)
      false)))
    

(defn -people-logged-in [users]
  (->> users
       (map (comp :username :user))))

(def people-logged-in #(-people-logged-in @Users))

(defn -ip-to-sender-handle [users ip]
  (->> users
       (map (fn [param] {:user-handle (-> param :user :user-handle)
               :ip-addresses (-> param
                                 :sessions
                                 deref
                                 keys)}))
       (filter #(in? (:ip-addresses %) ip))
       first
       :user-handle))

(def ip-to-sender-handle #(-ip-to-sender-handle @Users %))
                                                     
