(ns mese-client.ui.main-form
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [mese-client.friends :refer [get-current-users]]))

(defn show-mainform [sessionid]
  (let [form (->
              (frame :width 800
                     :height 600
                     :on-close :dispose
                     :visible? true
                     :content (listbox :model (get-current-users sessionid))))]
    true))
