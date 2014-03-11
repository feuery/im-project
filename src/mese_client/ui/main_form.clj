(ns mese-client.ui.main-form
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [mese-client.ui.listcontrol :refer [person-listbox]]
            [mese-client.ui.discussion :refer [discussion-form]]
            [mese-client.friends :refer [get-current-users]]))

(def user-poll-timespan 10) ;Seconds

(defn people-logged-in [sessionid]
  (get-current-users sessionid))

(defn string-renderer [f]
 (seesaw.cells/default-list-cell-renderer
  (fn [this {:keys [value]}] (.setText this (str (f value)))))))

(defn show-mainform [sessionid]
  (let [userseq (atom (people-logged-in sessionid))        
        form (frame :width 800
                     :height 600
                     :on-close :dispose
                     :visible? true
                     :content  (vertical-panel :items [(scrollable (-> (listbox
                                                                        :model (first @userseq)
                                                                        :renderer (string-renderer :username)
                                                                        :listen [:selection (fn [e]
                                                                                              (discussion-form sessionid (selection e)))])
                                                                       (config! :id :users)))]))]
    (doto (Thread.
           (fn []
             (reset! userseq (people-logged-in sessionid))
             (Thread/sleep (* user-poll-timespan 1000))
             (if (visible? form)
               (recur))))
      .start)

    (add-watch userseq :update-thing (fn [_ _ _ new-val]
                         (println "Caught new-val: " new-val)
                         (config! (select form [:#users]) :model new-val)))
    true))
