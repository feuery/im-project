(ns mese-test.db
  (:require [clojure.java.jdbc :as sql]
            [korma.db :refer :all]
            [seesaw.core :refer [input]]
            [korma.core :refer :all]))

(let [pw (input "Tietokannan salasana?")]
  (def dbspec {:classname "org.postgresql.Driver"
               :subname "//localhost:5432/yool-im"
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

(select data)

(defn serialize [key data-object]
  (insert data
          (values {:entity_name key
                   :data (pr-str data-object)})))

(defn de-serialize [key]
  (-> data
      select
      first
      :data
      read-string))
  
