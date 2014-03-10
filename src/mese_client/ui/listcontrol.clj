(ns mese-client.ui.listcontrol
  (:require [seesaw.core :refer :all]
            [seesaw.make-widget :refer [MakeWidget]]
            [seesaw.bind :as b]
            [seesaw.border :as border]
            [mese-test.util :refer [seq-in-seq? in?]]
            [merpg.2D.core :as c])
  (:import [java.net URL]
           [java.awt Dimension]))

(def user-keys [:user-handle :username :password :img-url :state :personal-message])
(def possible-states [:online :busy :away :returning :lunch :fake-offline :real-offline])

(defn state-to-color [state]
  {:pre [(in? possible-states state)]}
  (cond
   (in? [:online] state) "#1AFF00"
   (in? [:busy] state) "#FF0000"
   (in? [:away :returning :lunch]) "FFA600"
   :t "#999999"))

(defn string-renderer [f]
 (seesaw.cells/default-list-cell-renderer
  (fn [this {:keys [value]}] (.setText this (str (f value))))))

(defn person-listbox [personlist]
  (listbox :model personlist :renderer (string-renderer :username)
           :listen [:selection (fn [e]
                                 (alert (selection e)))]))                       

(defn do-test [control]
  (frame :width 320
         :height 160
         :on-close :dispose
         :content control
         :visible? true)
  nil)
      
