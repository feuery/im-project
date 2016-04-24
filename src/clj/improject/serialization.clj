(ns improject.serialization
  (:require [korma.core :as k]
            [korma.db :refer [transaction]]
            [improject.db :as db]))

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
