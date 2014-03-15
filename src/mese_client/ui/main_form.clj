(ns mese-client.ui.main-form
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as s]
            [mese-client.ui.discussion :refer [discussion-form]]
            [mese-client.friends :refer [get-current-users
                                         possible-states
                                         state-to-color]]
            [mese-client.communications :refer [get-inbox]])
  (:import [java.net URL]))

(def user-poll-timespan 10) ;Seconds

(defn people-logged-in [sessionid]
  (get-current-users sessionid))

(defn string-renderer [f]
 (seesaw.cells/default-list-cell-renderer
  (fn [this {:keys [value]}] (.setText this (str (f value))))))

(def windows-clone (atom nil))
(def latest-selection (atom nil))

(defn list-selection [current-user windows sessionid e]
  (try
    (defn new-window []
      (try
        (let [usratom (atom (selection e))
              discussion-form-result (discussion-form current-user sessionid usratom)
              swapping (swap! windows assoc (-> e selection :user-handle)
                              {:user usratom
                               :window (:window discussion-form-result)
                               :discussion (:discussion discussion-form-result)})]
          (reset! windows-clone swapping))
        (catch Exception ex
          (println "ex@listselection->new-window")
          (println ex))))
    
    ;; (println "List selection called with " (selection e))
                                        ;    (reset! latest-selection (selection e))
    (let [user-handle (:user-handle (selection e))]
      (if (contains? @windows user-handle)               
        (invoke-later
         (println "Window found")
         (try
           (if (visible? (:window (get @windows user-handle)))
             (do
               (println "Bringing window to front...")
               (try
                 (doto
                     (:window (get @windows user-handle))
                   (.toFront)
                   (.repaint))
                 (catch Exception ex
                   (println "Brining to front failed. Class (" (class (:window (get @windows user-handle))))
                   (println ex))))
             (do
               (println "Not bringing window to front...")
               (new-window)))
           (catch IllegalArgumentException ex
             (println "IAE!")
             (println "window: " (:window (get @windows user-handle)))
             (println "windows: " @windows)
             (println ex))))
        (do
          (println "Window found not")
          (new-window))))
    (catch Exception ex
      (println "ex@list-selection")
      (println ex))))


(defn edit-user [user-atom]
  (try
    (frame :size [320 :by 240]
           :title "Propertyeditor"
           :visible? true
           :content (grid-panel :columns 2 :items
                                (-> (map (fn [[key val]]
                                       [(str key)
                                        (text :text (str val)
                                              :listen [:document
                                                       (fn [e]
                                                         (println "Swapping " key " to " (text e) " on " user-atom)
                                                         (swap! user-atom assoc key
                                                                (cond
                                                                 (number? val) (Long/parseLong (text e))
                                                                 (keyword? val) val
                                                                 :t (text e))))])])
                                         (dissoc @user-atom :user-handle :state))
                                    flatten
                                    vec)))
    (catch Exception ex
      (println "Plöp")
      (println ex))))

(defn state-renderer [state]
  (str
   (-> state
       str
       (s/replace #":" "")
       s/capitalize)
   (if (= state :returning) " Soon" "")))
                                                                                     
(defn show-mainform [sessionid current-user-atom]
  (let [userseq (atom (people-logged-in sessionid)) 
        windows (atom {})
        discussions (atom {})  ;;keys are user-handles, vals are returned by the (discussion-form) - fn.
        form (frame :width 800
                     :height 600
                     :on-close :dispose
                     :visible? true
                     :content
                     (top-bottom-split (horizontal-panel
                                              :items [(-> @current-user-atom
                                                          :img-url
                                                          URL.
                                                          make-widget
                                                          (config! :id :imagebox
                                                                   :background
                                                                   (state-to-color (:state @current-user-atom))
                                                                   :size [100 :by 120]))
                                                      (vertical-panel
                                                       :items
                                                       [(horizontal-panel :items
                                                                          [(label :text (:username @current-user-atom)
                                                                                  :font "ARIAL-BOLD-18"
                                                                                  :id :username)
                                                                           (combobox :model (filter #(not (= % :real-offline)) possible-states)
                                                                                     :id :state-combobox
                                                                                     :size [200 :by 25]
                                                                                     :renderer
                                                                                     (string-renderer
                                                                                      state-renderer))])
                                                        (horizontal-panel :items [(label :text (:personal-message @current-user-atom)
                                                               :font "ARIAL-15"
                                                               :id :personal-message)])])]
                                              :listen [:mouse-released (fn [_]
                                                                         (edit-user current-user-atom))])
                                                          
                                             (scrollable (listbox
                                                          :model (try
                                                                   (first @userseq)
                                                                   (catch IllegalArgumentException ex
                                                                     (println "IAE napattu")
                                                                     (println "userseq: " @userseq)))
                                                          :renderer (string-renderer :username)
                                                          :id :users
                                                          :listen
                                                          [:mouse-released
                                                           (partial list-selection current-user-atom windows sessionid)]))))]

    (b/bind current-user-atom (b/transform :state)
            (select form [:#state-combobox]))
    (b/bind (b/selection (select form [:#state-combobox]))
            (b/b-swap! current-user-atom #(assoc %1 :state %2)))
    
    (b/bind current-user-atom (b/transform :username)
            (b/property (select form [:#username]) :text))
    
    (b/bind current-user-atom (b/transform :personal-message)
            (b/property (select form [:#personal-message]) :text))

    (b/bind current-user-atom
              (b/transform #(try
                              ((comp state-to-color :state) %)
                              (catch Exception ex ;;Catch the typos in the state...
                                (println ex))))
              (b/property (select form [:#imagebox]) :background))
    (b/bind current-user-atom
            (b/transform #(-> % :img-url URL.))
            (b/property (select form [:#imagebox]) :icon))
    
    (doto (Thread. ;;Inbox-receiver-thread
           (fn []
             (try
               (println "Inbox receiving began")
               (loop [inbox (get-inbox sessionid)]
                 (println "inbox (" inbox ") received")
                 (doseq [{sender :sender :as msg} inbox]
                   (println "sender " sender)
                   (loop [{sender :sender :as msg} msg
                          counter 1]
                     (if-let [{discussion :discussion} (get @windows sender)] ;;This discussion key doesnt exist!
                       (do
                         (println "swapping to discussion " discussion)
                         (swap! discussion #(try
                                              (cons msg %)
                                              (catch NullPointerException ex
                                                (println "Null!")
                                                (println "msg " msg " % " %)
                                                (throw ex)))))
                       (let [usratom (->> @userseq
                                          (filter #(= (:user-handle %) sender))
                                          first
                                          atom)]
                         (println "Creating the discussion...")
                         (if (= counter 2)
                           (throw (Exception. "Infinite loop in the inbox-receiver-thread!")))
                         (swap! windows assoc sender {:user usratom
                                                      :window (:window (discussion-form current-user-atom sessionid usratom))})
                         (recur msg 2)))))
                 
                 (Thread/sleep (* user-poll-timespan 1000))
                 (println "recurring inboxthread")
                 (recur (get-inbox sessionid)))                 
               (catch Exception ex
                 (println "Inbox-thread broken")
                 (println ex)
                 (throw ex)))))
      .start)
    
    (doto (Thread. ;;User update-thread...
           (fn []
             (try
               (println "Starting")
               (loop []
                 (try
                   (let [new-users (filter
                                    #(not (= (:user-handle %) (:user-handle @current-user-atom)))
                                    (people-logged-in sessionid))]
                     (try
                       (reset! userseq new-users)
                       (catch Exception ex
                         (println "Käyttäjäpäivitys hajoaa resetin alla")
                         (println "new-users: " new-users)
                         (throw ex))))
                   (catch Exception ex
                     (println "Vai people-logged-inin alla?")
                     (throw ex)))
                 (println "Waiting")
                 (Thread/sleep (* user-poll-timespan 1000))
                 (if (visible? form)
                   (do
                     ;(println "Recurring")
                     (recur))
                   (println "Failing - not visible? form")))
               (catch Exception ex
                 (println "Käyttäjäpäivitys on rikki")
                 (println ex)))))
      .start)

    (add-watch userseq :update-thing (fn [_ _ _ new-val]
                                       ;(println "Caught new-val: " new-val)
                         (config! (select form [:#users]) :model new-val)
                         (let [person-atoms (->> @windows
                                                 (map second)
                                                 (map vals)
                                                 (map first)
                                                 (filter #(instance? clojure.lang.Atom %)))]
                           (doseq [new-user new-val]
                             (try
                               (if-let [user (->> person-atoms
                                                  (filter #(= (:user-handle (deref %)) (:user-handle new-user)))
                                                  first)]
                                 (reset! user new-user)
                                 (do nil
                                   ;(println "new-user: " new-user)
                                   ;(println "User " (:user-handle new-user) " not found from " person-atoms "\nWindows: ")
                                        ;(pprint @windows)
                                   ))
                               (catch Exception ex
                                 (.printStackTrace ex)
                                 (throw ex)))))))
    true))
