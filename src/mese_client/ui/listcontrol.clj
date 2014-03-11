(ns mese-client.ui.listcontrol
  (:require [seesaw.core :refer :all]
            [seesaw.make-widget :refer [MakeWidget]]
            [seesaw.bind :as b]
            [seesaw.border :as border]
            [mese-test.util :refer [seq-in-seq? in?]]
            ;[mese-client.ui.discussion :refer [discussion-form]
            [merpg.2D.core :as c])
  (:import [java.net URL]
           [java.awt Dimension]))

DON'T LOAD ME - NOT REMOVE FOR STATE-TO-COLOR AWAITING EXISTENCE

(def user-keys [:user-handle :username :password :img-url :state :personal-message])
(def possible-states [:online :busy :away :returning :lunch :fake-offline :real-offline])

(defn state-to-color [state]
  {:pre [(in? possible-states state)]}
  (cond
   (in? [:online] state) "#1AFF00"
   (in? [:busy] state) "#FF0000"
   (in? [:away :returning :lunch]) "FFA600"
   :t "#999999"))


(defn do-test [control]
  (frame :width 320
         :height 160
         :on-close :dispose
         :content control
         :visible? true)
  nil)
      
