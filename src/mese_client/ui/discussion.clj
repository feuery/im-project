(ns mese-client.ui.discussion
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [seesaw.make-widget :refer [MakeWidget]]
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


(def possible-states [:online :busy :away :returning :lunch :fake-offline :real-offline])

(defn state-to-color [state]
  {:pre [(in? possible-states state)]}
  (cond
   (in? [:online] state) "#1AFF00"
   (in? [:busy] state) "#FF0000"
   (in? [:away :returning :lunch]) "FFA600"
   :t "#999999"))

(defn discussion-form [session-id friend]
  {:pre [(seq-in-seq? user-keys (keys @friend))]}
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
                  [(label :text (str (:username @friend) "    -    (" (-> @friend :state str (s/replace #":" "")) ")") :font "ARIAL-BOLD-18")
                   (label :text (:personal-message @friend) :font "ARIAL-15")
                   (horizontal-panel
                    :items
                    [(border-panel :north (-> @friend
                                              :img-url
                                              (URL.)
                                              make-widget
                                              (config! :background (state-to-color (:state @friend))
                                                       :size [100 :by 120]))
                                   :south "Oma kuvasi, tai jotain")
                     (top-bottom-split (text :multi-line? true :editable? false
                                             :wrap-lines? true :id :discussion
                                             :minimum-size [800 :by 600])
                                       (border-panel :center (text :multi-line? true :wrap-lines? true)
                                                     :east (button :text "SEND" :listen [:action-performed
                                                                                         (fn [_]
                                                                                           (alert "Not implemented"))]))
                                       :divider-location 2/3)])]))]
      form)
    (catch Exception ex
      (println ex)
      (throw ex))))
