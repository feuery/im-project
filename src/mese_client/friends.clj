(ns mese-client.friends
  (:require [org.httpkit.client :as http]
            [clojure.pprint :refer :all]
            [mese-test.util :refer [in?]]                        
            [mese-client.settings :refer [server-url]]))

(defn get-current-users [session-id]
  (let [{body :body :as result} @(http/get (str server-url "list-friends/" session-id))]
    ;; (println "Results: ")
    ;; (pprint result)
    (println "@get-current-users: body: " body)
    (if-let [result (read-string body)]
      (if (map? result)
        (do
          (println "Result was a map: " result)
          false)
        result))))

(def possible-states [:online :busy :away :returning :lunch :fake-offline :real-offline])

(defn state-to-color [state]
  {:pre [(in? possible-states state)]}
  (cond
   (in? [:online] state) "#1AFF00"
   (in? [:busy] state) "#FF0000"
   (in? [:away :returning :lunch] state) "#FFA600"
   :t "#999999"))
