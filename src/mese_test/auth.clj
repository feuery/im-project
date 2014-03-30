(ns mese-test.auth
  (:require [mese-test.user :refer [create-user
                                    get-outbox!
                                    get-inbox!]]
            [com.ashafa.clutch :as c]
            [clojure.pprint :refer :all]
            [mese-test.util :refer [in?
                                    to-number]]))

;(def Users (atom []))

(def db (c/get-database "yool-im-users"))
(def friend-db (c/get-database "yool-im-friends"))
(def request-db (c/get-database "yool-im-requests"))

(defn sha-512 [data]
  (let [md (. java.security.MessageDigest getInstance "sha-512")]
    (. md update (.getBytes data))
    (let [bytes (. md digest)]
      (reduce #(str %1 (format "%02x" %2)) "" bytes))))

(defn get-users []
  (->> (c/all-documents db)
       (map (comp (partial c/get-document db) :id))
       (map #(assoc % :state (keyword (:state %))))))

(def Users2 (atom {} :validator #(or (empty? %)
                                     (->> (vals %)
                                          (every? (fn [el] (contains? el :password)))))))  ;;Keys are :user-handles, values are the same things as in the old Users-atom
(defn add-user! [& params]
  (let [usr (apply create-user params)]
    (println "usr: " usr)
    (c/put-document db usr :id (:user-handle usr))
    (swap! Users2 assoc (:user-handle usr) usr)))

(when (empty? (get-users))
  (add-user! "feuer" "Feuer" "http://3.bp.blogspot.com/_z3wgxCQrDJY/S6CgYhXSkyI/AAAAAAAAAAg/0Vv0ffa871g/S220/imagex100x100.jpeg" :online (sha-512 "testisalasana"))
  (add-user! "new" "moimaailma" "http://prong.arkku.net/MERPG_logolmio.png" :online (sha-512 "testisalasana"))
  (add-user! "thrd" "Kolmas kaveri" "http://prong.arkku.net/MERPG_logolmio.png" :online (sha-512 "testisalasana")))

(doseq [usr (get-users)]
  (get-outbox! (:user-handle usr))
  (get-inbox! (:user-handle usr)))

(defn update-user! [usr]
  (c/update-document db usr))

(defn find-user
  "Doesn't work on the Users - atom"
  [user-db user-handle]
  (try
    (or
     (first (filter #(= (:user-handle %) user-handle) user-db))
     false)
    (catch NullPointerException ex
      (println "NPE in find-user")
      (throw ex))))

(def Friends
  "An atomified map containing all the friends of a single user. Key is the current user's user-handle, and value is a seq of friend's user-handles"
  (atom {}))

;(reset! Friends {})
(add-watch Friends :friend-serializer
           (fn [_ _ _ new-state]
             (doseq [[user-handle friend-seq] new-state]
               (if (c/document-exists? friend-db user-handle)
                 (let [doc (c/get-document friend-db user-handle)
                       new-doc (assoc doc (keyword user-handle) friend-seq)]
                   (println "updating new-doc: " new-doc)
                   (c/update-document friend-db new-doc))
                 (do
                   (println "Inserting")
                   (c/put-document friend-db {user-handle friend-seq} :id user-handle))))))

(defn add-as-friend! [user-handle friend-handle]
  {:pre [(and (contains? @Users2 friend-handle)
              (contains? @Users2 user-handle))]}
  (swap! Friends (fn [old]
                   (let [friend-set (into #{} (or (get old user-handle)
                                                  #{}))]
                     (assoc old user-handle (conj friend-set friend-handle))))))

(let [data (->> (c/all-documents friend-db)
		     (map :id)
		     (map (partial c/get-document friend-db))
		     (map (fn [val]
			    {(:_id val) (set (get val (keyword (:_id val))))})))]
  (when-not (empty? data)
    (swap! Friends into  (reduce into data))))

(defn create-friend-request [requester]
  {:requester requester :accepted? false})

(def Requests
  "Map, where keys are userhandles of those requested to be friends, and values are the requests with keys :requester and accepted."
  (atom {}))

(let [data (->> (c/all-documents request-db)
                (map :id)
                (map (partial c/get-document request-db))
                (map (fn [val]
                       {(:_id val) (set (get val (keyword (:_id val))))})))]
  (when-not (empty? data)
    (swap! Requests into (reduce into data))))

(add-watch Requests :request-serializer
           (fn [_ _ _ new-state]
             (doseq [[user-handle request-seq] new-state]
               (if (c/document-exists? request-db user-handle)
                 (let [doc (c/get-document request-db user-handle)
                       new-doc (assoc doc (keyword user-handle) (set request-seq))]
                   (println "updating: " new-doc)
                   (c/update-document request-db new-doc))
                 (let [doc {user-handle (set request-seq)}]
                   (println "inserting " doc)
                   (c/put-document request-db doc :id user-handle))))))

(defn translate-requests
  "Translates Requests - map into form which makes updating the Friends - map easier"
  [rq]
  (if-not (empty? rq)
    (->> rq
       (map (fn [mapentry]
              (map (fn [requestmap]
                     (assoc requestmap :requested (first mapentry))) (second mapentry))))
       (reduce into)
       (filter :accepted?))
    []))

(add-watch Requests :request-accepter
           (fn [_ _ _ new-state]
             (doseq [{requested :requested
                      requester :requester} (translate-requests new-state)]
               (println "Adding " requested " as " requester "'s friend")
               (add-as-friend! requested requester))))
             

(defn add-friend-request! [requested request]
  (swap! Requests (fn [old]
                   (if-let [request-list (set (get old requested))]
                     (assoc old requested (conj request-list request))
                     (assoc old requested #{request})))))

(defn requests-of [user-handle]
  (get @Requests user-handle []))

(defn accept-request [accepter requester]
  (swap! Requests (fn [old]
                    (if-let [request-set (get old accepter)]
                      (let [relevant-request  (into {} (filter
                                               #(= (:requester %) requester)
                                               request-set))
                            other-requests (vec (filter #(not= % relevant-request) request-set))]
                        (assoc old accepter (set (conj other-requests (assoc relevant-request :accepted? true)))))
                      old))))
                         

(defn friend?
  "This function declares user1 and user2 friends even though there exists only link for user1"
  [userhandle1 userhandle2]
  (boolean
   (if (= userhandle1 userhandle2)
     false
     (loop [userhandle1 userhandle1
            userhandle2 userhandle2
            recurring? false]
       (if recurring?
         (and (contains? @Friends userhandle1)
              (-> (get @Friends userhandle1)
                  (in? userhandle2)))
         (or
          (and (contains? @Friends userhandle1)
               (-> (get @Friends userhandle1)
                   (in? userhandle2)))
          (recur userhandle2 userhandle1 true)))))))

(defn -friends-of [user-db user-handle]
  (filter (partial friend? user-handle) (keys user-db)))
      

(def friends-of #(-friends-of @Users2 %))
   
(swap! Users2 into (->> (get-users)
                         (map (fn [{user-handle :user-handle :as user}]
                                {user-handle user}))))



(defn find-user-real
  "Works on the Users - atom"
  [user-handle & {:keys [with-sessions?] :or {with-sessions? false}}]
  (or
   (let [user (get @Users2 user-handle)]
     (do
       (println "real-user: " user "| with-sessions? " with-sessions?)
       (if with-sessions?
         (dissoc user :password) 
         (dissoc user :password :sessions))))
   false))

(defn commit-user! [user-handle user]
  (swap! Users2 (fn [old]
                  (assoc old user-handle
                         (assoc user :password (:password (get old user-handle)))))))

(defn session-belongs-to-user? [user session-id]
  {:pre [(in? (keys user) :sessions)]}
                                        ;(println "PLÄÄ PLÖÖ PLÖÖ PLYY")
;  (println "in session-belongs-to-user?: " user)
  (let [session-ids (->> user
                         :sessions
                         deref)]
 ;   (println "first session-ids: " session-ids)
    (let [session-ids (->> session-ids
                           (map second)
                           (map :session-id)
                           (map str))]
      ;; (println "second session-ids: " session-ids)
      ;; (println "(in? " session-ids " " (str session-id) ")")
      (in? session-ids (str session-id)))))

(defn user-authenticates? [user-db username naked-password]
  (let [password (sha-512 naked-password)
        user (find-user user-db username)]
    (pprint user-db)
    (println "user: " user)
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
  {:post [(let [{sessions :sessions} (get @Users2 username)]
            (every? #(= (count  %) 2) (vals @sessions)))]}
  
  (println "Authing 1st pass ")
  (println "(user-authenticates!? " (pr-str (vec (vals @Users2))) " " username " " naked-password ")")
  (if (user-authenticates? (vals @Users2) username naked-password) ;;userdb:stä löytyy username, jonka salasana mätsää annettuun
    (do
      (println "Authed 1st pass")
      (when-let [user (find-user (vals @Users2) username)]
        (println "found user: " user)
        (if-not (in? (->> @Users2 vals (map :user)) user)
          (let [session-id (get-sessionid!)]
            (println username " signed in for the first time") 
            (swap! Users2 assoc username (into user {:sessions (atom {ip {:last-call (System/currentTimeMillis)
                                                                          :session-id session-id}})}))
            (println "delivering " session-id)
            (deliver sessionid-promise session-id))
          (let [{sessions :sessions} (get @Users2 username)
                old-val (get @sessions ip)
                new-val (assoc old-val :last-call (System/currentTimeMillis))]
            (println username " signed in again") 
            (let [toret (swap! sessions assoc ip new-val)
                  session-id-tmp (-> toret
                                     (get ip))
                  session-id (:session-id session-id-tmp)]
              (comment              (println "the new sessions: " @sessions)
                                    (println "toret" toret))
              (println "Session-id: " session-id-tmp)
              (println "sessid:     " session-id)
              
              (deliver sessionid-promise session-id))))
        (println "Returning true...")
        true))
    (do
      (println "Returning false")
      (println "(user-authenticates? " (pr-str (vec (vals @Users2))) " " (pr-str username) " " (pr-str naked-password)")")
      false)))

(defn -logout! [sessionid ip users]
  (try
    (println "Logging out.... ")
    ;; (pprint users)
    ;; (println "sessid: " (class sessionid) "; ip: " (class ip))
    (let [filtered-stuff (->> users
                              (filter #(and
                                          (contains? % :sessions)
                                          (do
                                            (println "Contains sessions")
                                            true)
                                          (contains? (-> %
                                                         :sessions
                                                         deref) ip)
                                          (do
                                            (println "contains ip")
                                            true)
                                          (= (-> % :sessions deref (get ip) :session-id str) sessionid)
                                          (do
                                            (println "sessionid:t mätsäävät")
                                            true))))]
      (println "Filtered stuff: " filtered-stuff)
      (if (empty? filtered-stuff)
        (do
          (println "(empty filtered-stuff)")
          false)
        (do
          (println "(not (empty? filtered-stuff): " filtered-stuff)
          (swap! (-> filtered-stuff
                     first
                     :sessions)
                 (fn [old]
                   (assoc old ip (assoc (get old ip) :last-call 0))))
          true)))
    (catch Exception ex
      (println "EX@-logout: " ex)
      false)))

(def logout! #(-logout! %1 %2 (vals @Users2)))

(defn session-timeout? [session-mapentry]
  ;; Session-mapentry = [ip {:last-call :sessid}]
  (try
    (let [five-min-in-ms (* 60 1000)]
      (< (- (System/currentTimeMillis)
            (-> session-mapentry second :last-call))
         five-min-in-ms))
    (catch Exception ex
      (println ex)
      (println "Session-mapentry: " session-mapentry))))

(defn rename-user [users user-handle new-username]
  (when-let [staty-user (get users user-handle)]
    (assoc users user-handle
           (assoc staty-user :user
                  (assoc (:user staty-user) :username new-username)))))

(defn rename-user! [user-handle new-username]
  (swap! Users2 rename-user user-handle new-username))

(defn -user-logged-in? [username users]
  (try
    (let [username-found? (contains? users (:user-handle username))
                                        ;_ (println "Täällä ollaan!")
          {sessions :sessions :as user} (get users (:user-handle username))]
                                        ;_ (println "users: " user)
      (if (nil? sessions)
        false
        (let [session-not-timed-out?  (every? session-timeout? @sessions)]
          ;(pprint @sessions) 
          (and username-found? session-not-timed-out?))))
    (catch NullPointerException ex
      (println ex)
      (throw ex))))

(def user-logged-in? #(-user-logged-in? %  @Users2))

(defn logged-out [user]
  (println "user: " user)
  (if-not (user-logged-in? user)
    (do (println "User " user " logged out")
        (assoc user :state :real-offline))
    (do (println "Users " user " logged in")
        user)))

(defn session-authenticates? [ip session-id]
  (try
    (let [sessions (->> @Users2
                        vals
                        (filter :sessions)
                        (map (comp deref :sessions))
                        (reduce merge))
          relevant-atom (->> @Users2
                             vals
                             (map :sessions)
                             (filter #(contains? @% ip))
                             first) ;; Shame on you if signing twice from the same ip...
          old-val (get @relevant-atom ip)
          new-val (assoc old-val :last-call (System/currentTimeMillis))
          five-min-in-ms (* 60 1000)]      
      
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
    

(def people-logged-in #(vals @Users2))

(defn -username->userhandle [users username]
  (->> users
       (filter #(= (-> % :user :user-handle) username))
       first
       :user
       :username))

(def username->userhandle #(-username->userhandle (vals @Users2) %))
   
(defn -ip-to-sender-handle [users ip]
  (->> users
       vals
       (map (fn [param] {:user-handle (-> param :user-handle)
                         :ip-addresses (-> param
                                 :sessions
                                 deref
                                 keys)}))
       (filter #(in? (:ip-addresses %) ip))
       first
       :user-handle))

(def ip-to-sender-handle #(-ip-to-sender-handle @Users2 %))


(defn -sessionid->userhandle [user-db sessionid]
  (->> user-db
       vals
       (filter (fn [{sessions :sessions}]
                 (if (nil? sessions)
                   false
                   (let [reduced-sessions (->> @sessions
                                               (map second)
                                               (map vals)
                                               (reduce into))]
                     (in? reduced-sessions sessionid)))))
       first
       :user-handle))

(def sessionid->userhandle #(-sessionid->userhandle @Users2 %))
