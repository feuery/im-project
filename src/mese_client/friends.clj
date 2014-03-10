(ns mese-client.friends
  (:require [org.httpkit.client :as http]
            [clojure.pprint :refer :all]
            [mese-client.settings :refer [server-url]]))

(defn get-current-users [session-id]
  (let [{body :body :as result} @(http/get (str server-url "list-friends/" session-id))]
    ;; (println "Results: ")
    ;; (pprint result)
    ;(println "body: " body)
    (if-let [result (read-string body)]
      (if (map? result)
        (do
          (println "Result was a map: " result)
          false)
        result))))
