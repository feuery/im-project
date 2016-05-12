(ns improject.inboxes
  (:require [improject.schemas :as schemas]
            [schema.core :as s]
            [clojure.pprint :refer [pprint]]
            [clj-time.core :as t]))

(defn in? [col val]
  (some (partial = val) col))

(def inboxes (atom {}
                   :validator (partial s/validate schemas/inbox-schema)))

;; (add-watch inboxes :pprinter (fn [_ _ _ new]
;;                                (println "Inboxes: ")
;;                                (pprint new)))

(defn send-to! [recipient message]
  (when-not (contains? @inboxes recipient)
    (swap! inboxes assoc recipient []))
  ;; (println "-------------------------")
  ;; (pprint message)
  ;; (println "-------------------------")
  (swap! inboxes update-in [recipient] (comp vec conj) message)
  (swap! inboxes update-in [recipient] (comp vec (partial sort-by :date))))

(defn inbox-of! [whose conv-partner session-id session-ids-atom]
  (when-let [session-ids (get @session-ids-atom whose)]
    (when (in? session-ids session-id)
      (let [inbox (get @inboxes whose)
            already-sent (filter #(or (in? (:sent-to %) session-id)
                                      (not (in? [whose conv-partner] (:recipient %))))
                                 inbox)
            not-sent (->> inbox
                          (filter (complement (partial in? already-sent)))
                          (sort-by :date)
                          vec)]
        ;; (println "There's something fishy on the following line")
        (swap! inboxes update whose (comp vec
                                          (fn [inbox]
                                            (->> inbox
                                                 (map (fn [message]
                                                        (if (in? (:sent-to message)
                                                                 session-id)
                                                          message
                                                          (update message :sent-to conj session-id))))
                                                 vec))))
        ;; (println "asdmoi")
        (swap! inboxes update whose (comp vec (partial sort-by :date)))
        not-sent))))


(comment
  (send-to! "feuer" {:message "Asdmoi"
                     :sender {:username "kolmas hahmo" 
                              :displayname "kolmas hahmo"
                              :img_location "http://4everstatic.com/pictures/80x80/animals/wildlife/red-panda-152735.jpg"
                              :personal_message "Terve kaikki"
                              :font_name "asd"
                              :bold true
                              :italic true
                              :underline false
                              :color "#FF0000"}
                     :date (t/now)
                     :sent-to []})
  (send-to! "feuer" {:message "Asdmoi"
                     :sender {:username "kolmas hahmo" 
                              :displayname "kolmas hahmo"
                              :img_location "http://4everstatic.com/pictures/80x80/animals/wildlife/red-panda-152735.jpg"
                              :personal_message "Terve kaikki"
                              :font_name "asd"
                              :bold true
                              :italic true
                              :underline false
                              :color "#FF0000"}
                     :date (t/now)
                     :sent-to []})

  (def session-ids (atom {}
                         :validator (partial s/validate schemas/session-id-schema)))

  (def id (keyword (gensym)))
  (swap! session-ids assoc "feuer" [id])
  
  (-> (inbox-of! "feuer" id session-ids) pprint))
