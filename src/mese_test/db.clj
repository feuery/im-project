(ns mese-test.db
  (:require [clojure.java.jdbc :as sql]
            [korma.db :refer :all]
            [seesaw.core :refer [input]]
            [korma.core :refer :all]))

(let [pw (System/getenv "db_pw")]
  (def dbspec {:classname "org.postgresql.Driver"
               :subname "//localhost:5432/copper"
               :subprotocol "postgresql"
               :user "feuer"
               :password pw})


  (defdb db (postgres {:db "yool-im"
                       :user "feuer"
                       :password pw})))


(defn create-data-table []
  (when (empty? (sql/query dbspec "SELECT * FROM pg_catalog.pg_tables WHERE tablename = 'data'"))
    (sql/db-do-commands dbspec "CREATE TABLE data
(
  entity_name character varying(255) NOT NULL,
  data text NOT NULL,
  CONSTRAINT data_pkey PRIMARY KEY (entity_name)
)")))

(defentity data )

;(select data)

(defn serialize [key data-object]
  (if (empty? (select data
                      (where {:entity_name key})))
    (do
      (println "Inserting stuff to " key)
      (insert data
              (values {:entity_name key
                       :data (pr-str data-object)})))
    (do
      (println "Updating stuff on " key)
      (update data
              (set-fields {:data (pr-str data-object)})
              (where {:entity_name key})))))

(defn de-serialize [key]
  (let [data (select data
                     (where {:entity_name key}))]
    (cond
     (empty? data) []
     :t (let [data (-> data
                       first
                       :data)]
          (println "deserializing " data)
          (read-string data)))))
  
