(ns improject.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(def data (r/atom {:outbox []
                   :inbox []}))

(register-sub :no-users
              (fn [db _]
                (reaction (get-in @db [:no-users]))))

(register-sub :location
              (fn [db _]
                (reaction (get-in @db [:location])))) 

(register-sub :username
              (fn [db _]
                (reaction (get-in @db [:user-model :username]))))

(register-sub :friend-list
              (fn [db _]
                (reaction (get @db :friend-list))))
(register-sub :user-model
              (fn [db _]
                (reaction (get @db :user-model))))

(register-sub :conversation-partner
              (fn [db _]
                (reaction (get @db :conversation-partner))))

(register-sub :conversation
              (fn [db _]
                (reaction (get @db :inbox))))

(register-sub :all-users
              (fn [db _]
                (reaction (get @db :all-users))))

(register-sub :filtered-users
              (fn [db _]
                (reaction (get @db :filtered-users))))

(register-sub :requests
              (fn [db _]
                (reaction (get @db :requests))))

(.log js/console "improject.state loaded") 

