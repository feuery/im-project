(ns mese-test.auth
  (:require [mese-test.user :refer [create-user
                                    get-outbox!
                                    get-inbox!]]
            [mese-test.util :refer [in?]]))

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

(defn get-new-sessionid [session-ids]
  (loop [session-id (rand-int Integer/MAX_VALUE)]
    (if (in? session-ids session-id)
      (recur (rand-int Integer/MAX_VALUE))
      session-id)))

(defn user-authenticates!?
  "If this returns true, the user is marked logged in"
  [username naked-password ip sessionid-promise]
  (if (user-authenticates? user-db username naked-password)
    (let [user (find-user user-db username)
          session-ids (->> @Users
                          (map (comp deref :sessions))
                          flatten)
          session-id (get-new-sessionid session-ids)]
      (if-not (in? (map :user @Users) user)
        (do
          (println username " signed in for the first time")
          (swap! Users conj {:user user :sessions (atom {session-id ip})}))
        (let [{sessions :sessions} (->> @Users
                                        (filter #(= (-> %
                                                        :user
                                                        :user-handle) username))
                                        first)]
          (println username " signed in again")
          (swap! sessions assoc session-id ip)))
      (deliver sessionid-promise session-id)
      true)
    false))

(defn session-authenticates? [sessionid ip]
  (let [sessions (->> @Users
                      (map (comp deref :sessions))
                      (reduce merge))]
    (if (contains? sessions sessionid)
      (println "sessions contains " sessionid)
      (do
        (println "sessions " sessions " doesn't contain " sessionid)
        (pprint sessions)
        (println "sessioluokka: " (class sessions))))
      
    (if (= ip (get sessions sessionid))
      (println "ip " ip " == " (get sessions sessionid))
      (println "ip " ip " != " (get sessions sessionid)))
    (and (contains? sessions sessionid)
         (= ip (get sessions sessionid)))))

(defn -people-logged-in [users]
  (->> users
       (map (comp :username :user))))

(def people-logged-in #(-people-logged-in @Users))
