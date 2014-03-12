(ns mese-client.ui.main-form
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [clojure.pprint :refer [pprint]]
            [mese-client.ui.discussion :refer [discussion-form]]
            [mese-client.friends :refer [get-current-users]]))

(def user-poll-timespan 10) ;Seconds

(defn people-logged-in [sessionid]
  (get-current-users sessionid))

(defn string-renderer [f]
 (seesaw.cells/default-list-cell-renderer
  (fn [this {:keys [value]}] (.setText this (str (f value))))))

(defn list-selection [windows sessionid e]
  (defn new-window []
    (let [usratom (atom (selection e))]
      (swap! windows assoc (-> e selection :user-handle) {:user usratom
                                                          :window (discussion-form sessionid usratom)})))
  ;; (println "List selection called with " (selection e))
  (if (contains? @windows (selection e))
    (invoke-later
     (println "Window found")
     (if (visible? (:window (get @windows (selection e))))
       (doto
           (:window (get @windows (selection e)))
         (.toFront)
         (.repaint))
       (new-window)))
    (do
      (println "Window not found")
      (new-window))))
                                                                                     
(defn show-mainform [sessionid]
  (let [userseq (atom (people-logged-in sessionid))
        windows (atom {})
        form (frame :width 800
                     :height 600
                     :on-close :dispose
                     :visible? true
                     :content
                     (vertical-panel :items [(scrollable (listbox
                                                          :model (first @userseq)
                                                          :renderer (string-renderer :username)
                                                          :id :users
                                                          :listen
                                                          [:mouse-released
                                                           (partial list-selection windows sessionid)]))]))]
    (doto (Thread.
           (fn []
             (try
               (println "Starting")
               (loop []
                 (try
                   (let [new-users (people-logged-in sessionid)]
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
                     (println "Recurring")
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
                                 (do
                                   (println "new-user: " new-user)
                                   (println "User " (:user-handle new-user) " not found from " person-atoms "\nWindows: ")
                                        ;(pprint @windows)
                                   ))
                               (catch Exception ex
                                 (.printStackTrace ex)
                                 (throw ex)))))))
    true))

(comment [{:user {:user-handle "feuer", :username "Feuer", :password "68264921dadf2a507baf7805559571ec7c585afe64f6d7127e013d07ce2cc30492426feaeb42fa9b89dff7ee2d39a61a5079135699434893281f39d041c6d43f", :img-url "http://3.bp.blogspot.com/_z3wgxCQrDJY/S6CgYhXSkyI/AAAAAAAAAAg/0Vv0ffa871g/S220/imagex100x100.jpeg", :state :online, :personal-message "This is a personal message"}, :sessions #<Atom@453a7cca: {"127.0.0.1" {:last-call 1394614537113, :session-id 7179651}}>}])
