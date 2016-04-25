(ns improject.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(def data (r/atom {:username ""
                   :outbox []
                   :inbox []}))

;; (def username (reaction (:username @data)))
;; (def inbox (reaction (:inbox @data)))


(register-sub :username
              (fn [db [sid & _]]
                (assert (= sid :username))
                (reaction (get-in @db [:username]))))

(register-sub :no-users
              (fn [db _]
                (reaction (get-in @db [:no-users]))))

(register-sub :location
              (fn [db _]
                (reaction (get-in @db [:location])))) 

(.log js/console "improject.state loaded")
