(ns improject.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(def data (r/atom {:sessionid -1
                   :username ""
                   :outbox []
                   :inbox []}))

;; (def sessionid (reaction (:sessionid @data)))
;; (def username (reaction (:username @data)))
;; (def inbox (reaction (:inbox @data)))

(register-sub :sessionid
              (fn [db [sid & _]]
                (assert (= sid :sessionid))
                (reaction (get-in @db [:sessionid]))))

(register-sub :username
              (fn [db [sid & _]]
                (assert (= sid :username))
                (reaction (get-in @db [:username]))))
                   
