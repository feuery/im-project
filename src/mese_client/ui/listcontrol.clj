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

(comment Broken...
         (defn listcontrol [person-atom]
           {:pre [seq-in-seq? user-keys (keys @person-atom)]}
           (let [img (c/image (:img-url @person-atom))
                 ;;Draw the buffer!
                 toret (border-panel :west (-> (URL. (:img-url @person-atom))
                                               make-widget
                                               (config! :border (border/line-border :thickness 10 :color "#FF0000"))
                                               (config! :id :imgview))
                                     :east (vertical-panel :items [(label :id :usrname :text (:username @person-atom))
                                                                   (label :id :persmsg :text (:personal-message @person-atom))])
                                     :size [(c/width img) :by (c/height img)])]
             (b/bind person-atom (b/transform :username)  (b/property (select toret [:#usrname]) :text))
             (b/bind person-atom (b/transform :personal-message) (b/property (select toret [:#persmsg]) :text))
             (add-watch person-atom :imageview (fn [_ _ _ new-val]
                                                 (config! (select toret [:#imgview]) :icon (:img-url new-val))))
             toret))
         (extend-type User
           MakeWidget
           (make-widget* [user]
             (println "Making widget of user...")
             (listcontrol user)))

         (defn to-viewable-user [user]
           (merge (User. nil nil nil nil nil) user)))

(defn string-renderer [f]
 (seesaw.cells/default-list-cell-renderer
  (fn [this {:keys [value]}] (.setText this (str (f value))))))

(defn person-listbox [personlist]
  (listbox :model personlist :renderer (string-renderer :username)
           :listen [:selection (fn [e]
                                 (alert (selection e)))]))
  
  (comment  (let [listcontrols (map (comp listcontrol #(merge (User. nil nil nil nil nil) %) person-atoms)]
    listcontrol)))
;    (vertical-panel :items listcontrols)))
                       

(defn do-test [control]
  (frame :width 320
         :height 160
         :on-close :dispose
         :content control
         :visible? true)
  nil)
      
