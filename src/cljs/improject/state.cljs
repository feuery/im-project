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
