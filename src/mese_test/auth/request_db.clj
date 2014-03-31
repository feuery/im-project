(ns mese-test.auth.request-db
  (:require [mese-test.user :refer [create-user
                                    get-outbox!
                                    get-inbox!]]
            [mese-test.auth.friend-db :refer [add-as-friend!]]
            [com.ashafa.clutch :as c]
            [clojure.pprint :refer :all]
            [mese-test.auth :refer [sha-512]]
            [mese-test.util :refer [in?
                                    to-number]]))

(def request-db (c/get-database "yool-im-requests"))

(def Requests
  "Map, where keys are userhandles of those requested to be friends, and values are the requests with keys :requester and accepted."
  (atom {}))

(let [data (->> (c/all-documents request-db)
                (map :id)
                (map (partial c/get-document request-db))
                (map (fn [val]
                       {(:_id val) (set (get val (keyword (:_id val))))})))]
  (when-not (empty? data)
    (swap! Requests into (reduce into data))))

(add-watch Requests :request-serializer
           (fn [_ _ _ new-state]
             (doseq [[user-handle request-seq] new-state]
               (if (c/document-exists? request-db user-handle)
                 (let [doc (c/get-document request-db user-handle)
                       new-doc (assoc doc (keyword user-handle) (set request-seq))]
                   (println "updating: " new-doc)
                   (c/update-document request-db new-doc))
                 (let [doc {user-handle (set request-seq)}]
                   (println "inserting " doc)
                   (c/put-document request-db doc :id user-handle))))))

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