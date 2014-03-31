(ns mese-test.auth.friend-db
  (:require [mese-test.user :refer [create-user
                                    get-outbox!
                                    get-inbox!]]
            [mese-test.auth.user-db :refer [Users2]]
            [com.ashafa.clutch :as c]
            [clojure.pprint :refer :all]
            [mese-test.auth :refer [sha-512]]
            [mese-test.util :refer [in?
                                    to-number]]))


(def friend-db (c/get-database "yool-im-friends"))

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

