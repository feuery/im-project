(ns mese-client.ui.discussion
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [clojure.string :as s]
            [seesaw.border :as border]
            [mese-test.util :refer [seq-in-seq? in?]]
            [merpg.2D.core :as c]
            [mese-client.communications :refer :all])
    (:import [java.net URL]
           [java.awt Dimension]))

(comment
            +-------------------------------------------------------+
            |   Your friend's name               (State)            |
            |   Friend's personal msg                               |
            +-------------------------------------------------------+
            | Your fri- |                                           |
            | end's img |                                           |
            +-----------+                                           |
            |           |                                           |
            |           |                                           |
            |           |       Discussion                          |
            |           |                                           |
            |           |                                           |
            |           +-------------------------------------------+
            |           |                                           |
            +-----------+                            -----------    |
            |           |                           (    SEND   )   |
            |           |                            -----------    |
            | Your pers-|        Your msg                           |
            | onal img  |                                           |
            |           |                                           |
            +-----------+-------------------------------------------+)

(def user-keys [:user-handle :username :img-url :state :personal-message])

(defn discussion-form [session-id friend]
  {:pre [(seq-in-seq? user-keys (keys friend))]}
  (try
    (println "Doing discussion")
    (let [form (frame
                :width 640
                :height 480 ;;TODO Setup a settings-repository for these, and update it on-resize
                :visible? true
                :on-close :dispose
                :content
                (vertical-panel
                  :items
                  [(str (:username friend) "    -    (" (-> friend :state str (s/replace #":" "")) ")")
                   (:personal-message friend)
                   (horizontal-panel
                    :items
                    [(border-panel :north (-> friend
                                              :img-url
                                              (URL.))
                                   :south "Oma kuvasi, tai jotain")
                     (top-bottom-split (text :multi-line? true :editable? false
                                             :wrap-lines? true :id :discussion)
                                       (border-panel :center (text :multi-line? true :wrap-lines? true)
                                                     :east (button :text "SEND" :listen [:action-performed
                                                                                         (fn [_]
                                                                                           (alert "Not implemented"))]))
                                       :divider-location 2/3)])]))]
      form)
    (catch Exception ex
      (println ex)
      (throw ex))))
