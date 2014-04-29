(ns mese-test.db
  (:require [clojure.java.jdbc :as sql]
            [korma.db :refer :all]
;            [seesaw.core :refer [input]]
            [korma.core :refer :all]
            [clojure.string :refer [split]]))

(defn parse-db-uri
  [uri]
  (drop 1 (split uri #"://|:|@|/")))

(defn create-map-from-uri
  [uri]
  (let [parsed (parse-db-uri uri)]
    (zipmap [:user :password :host :port :db] parsed)))

(defn db-info
  []
  (let [map (create-map-from-uri (System/getenv "DATABASE_URL"))]
    (assoc map
      :classname "org.postgresql.Driver"
      :subname (str "//" (:host map) "/" (:db map))
      :subprotocol "postgresql")))
   ; dev-db-info))

(defn create-data-table [dbspec]
  (println "dbspec: " dbspec)
  (when (empty? (sql/query dbspec "SELECT * FROM pg_catalog.pg_tables WHERE tablename = 'data'"))
    (sql/db-do-commands dbspec "CREATE TABLE data
(
  entity_name character varying(255) NOT NULL,
  data text NOT NULL,
  CONSTRAINT data_pkey PRIMARY KEY (entity_name)
)")))

(defn init []
  (let [pw (System/getenv "db_pw")] ;;This doesn't work on the heroku
    (println "the db_pw: " pw)
    (println "db_url: " (System/getenv "DATABASE_URL"))
    (def dbspec (db-info))
    (comment {:classname "org.postgresql.Driver"
              :subname "//localhost:5432/dfuv70squqtkjd"
              :subprotocol "postgresql"
              :user "fkarefreqqplnt"
              :password pw})
    (println "dbspec specs'd")
    (defdb db (postgres (db-info)))
    (comment 
               {:db "yool-im"
                :user "fkarefreqqplnt"
                :password pw}))
  (create-data-table dbspec))

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
  
