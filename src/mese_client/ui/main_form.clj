(ns mese-client.ui.main-form
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [mese-client.ui.listcontrol :refer [person-listbox]]
            [mese-client.friends :refer [get-current-users]]))

(defn people-logged-in [sessionid]
  (lazy-seq
   (cons (get-current-users sessionid)
         (people-logged-in sessionid))))

(defn show-mainform [sessionid]
  (let [userseq (atom (people-logged-in sessionid))        
        form (frame :width 800
                     :height 600
                     :on-close :dispose
                     :visible? true
                     :content  (vertical-panel :items [
                                                       (scrollable (person-listbox (first @userseq)))]))]
    true))
