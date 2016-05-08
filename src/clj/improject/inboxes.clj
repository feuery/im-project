(ns improject.inboxes
  (:require [improject.schemas :refer [inbox-schema]]
            [schema.core :as s]
            [clojure.pprint :refer [pprint]]
            [clj-time.core :as t]))

(def inboxes (atom {}
                   :validator (partial s/validate inbox-schema)))

(add-watch inboxes :pprinter (fn [_ _ _ new]
                               (println "Inboxes: ")
                               (pprint new)))

(defn send-to! [recipient message]
  (when-not (contains? @inboxes recipient)
    (swap! inboxes assoc recipient []))
  ;; (println "-------------------------")
  ;; (pprint message)
  ;; (println "-------------------------")
  (swap! inboxes update-in [recipient] (comp vec conj) message)
  (swap! inboxes update-in [recipient] (comp vec (partial sort-by :date))))
