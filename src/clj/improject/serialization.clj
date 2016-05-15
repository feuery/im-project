(ns improject.serialization
  (:require [korma.core :as k]
            [korma.db :refer [transaction]]
            [improject.db :as db]
            [improject.schemas :refer [user-schema]]
            [schema.core :as schemas]))

(defn save-user! [user-with-font]
  (let [font-keyset  [:color
                      :underline
                      :font_name
                      :italic
                      :bold]
        font (select-keys user-with-font font-keyset)
        user (apply (partial dissoc user-with-font) font-keyset)
        font-id (atom -1)]
    (transaction
     (let [font-name (:font-name font)
           fonts (k/select db/font_preference
                           (k/where font))]
       (if (empty? fonts)
         (reset! font-id (:id (k/insert db/font_preference
                                        (k/values font))))
         (reset! font-id (:id (first fonts)))))
     (k/insert db/users
               (k/values (assoc user :font_id @font-id))))))

(defn get-friends-of! [username]
  ;; TODO This should pick also friends whose friend you've been set as
  (let [query "SELECT u.*, fonts.*
FROM friendship f
LEFT JOIN users u ON f.username1 = u.username OR f.username2 = u.username
LEFT JOIN font_preference fonts ON fonts.id = u.font_id
WHERE ((f.username1 = ? OR f.username2 = ?) 
AND u.username <> ?)"]
    (->> (k/exec-raw [query [username username username]] :results)
         (map #(dissoc % :id :font_id :can_login))
         (map (partial schemas/validate user-schema))
         vec)))
                                

(defn in? [seq val]
  (some (partial = val) seq ))

(defn friends? [username1 username2]
  (let [user1-friends (get-friends-of! username1)
        user2-friends (get-friends-of! username2)]
    (or (in? user1-friends username2)
        (in? user2-friends username1))))

(defn make-friends! [username1 username2]
  (if (= 2
         (count 
          (k/select db/users
                    (k/where (or (= :username username1)
                                 (= :username username2))))))
    (if-not (friends? username1 username2)
      (k/insert db/friendship
                (k/values {:username1 username1 :username2 username2})))))

