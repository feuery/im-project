(ns mese-test.auth.request-db
  (:require [mese-test.user :refer [create-user
                                    get-outbox!
                                    get-inbox!]]
            [mese-test.auth.friend-db :refer [add-as-friend!]]
            [mese-test.db :refer [de-serialize serialize]]
            [clojure.pprint :refer :all]
            [mese-test.auth :refer [sha-512]]
            [mese-test.util :refer [in?
                                    to-number]]))

(def Requests
  "Map, where keys are userhandles of those requested to be friends, and values are the requests with keys :requester and accepted."
  (atom {}))

(let [data (de-serialize "requests")]
  (when-not (empty? data)
    (swap! Requests into data)))

(add-watch Requests :request-serializer
           (fn [_ _ _ new-state]
             (serialize "requests" new-state)))

(defn translate-requests
  "Translates Requests - map into form which makes updating the Friends - map easier"
  [rq]
  (if-not (empty? rq)
    (->> rq
       (map (fn [mapentry]
              (map (fn [requestmap]
                     (assoc requestmap :requested (first mapentry))) (second mapentry))))
       (reduce into)
       (filter :accepted?))
    []))

(add-watch Requests :request-accepter
           (fn [_ _ _ new-state]
             (doseq [{requested :requested
                      requester :requester} (translate-requests new-state)]
               (println "Adding " requested " as " requester "'s friend")
               (add-as-friend! requested requester))))
             

(defn add-friend-request! [requested request]
  (swap! Requests (fn [old]
                   (if-let [request-list (set (get old requested))]
                     (assoc old requested (conj request-list request))
                     (assoc old requested #{request})))))

(defn requests-of [user-handle]
  (get @Requests user-handle []))

(defn accept-request [accepter requester]
  (swap! Requests (fn [old]
                    (if-let [request-set (get old accepter)]
                      (let [relevant-request  (into {} (filter
                                               #(= (:requester %) requester)
                                               request-set))
                            other-requests (vec (filter #(not= % relevant-request) request-set))]
                        (assoc old accepter (set (conj other-requests (assoc relevant-request :accepted? true)))))
                      old))))

(defn create-friend-request [requester]
  {:requester requester :accepted? false})
