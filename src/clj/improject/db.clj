(ns improject.db
  (:require [korma.core :as k]
            [korma.db :as kd]
            [clojure.java.jdbc :as j]
            
            [config.core :refer [env]]
            [clojure.string :as s]
            [clojure.pprint :refer :all]
            [clojure.java.io :as io]))

(def db-username (or false (env :repl-user)))
(def db-password (or false (env :repl-password)))
(def db-url (env :database-url))

(defn init_sql! []
  (let [lines (-> "sql/schema.sql"
                  io/resource
                  slurp)
        fn-lines (->> (s/split lines #"\$\$ LANGUAGE plpgsql;")
                        (filter (partial not= "\n"))
                        (map #(str % "$$ LANGUAGE plpgsql;")))]
    (doseq [l fn-lines]
      (k/exec-raw l))
    (k/exec-raw "SELECT create_all();")))

(try
  (let [connection-map (->>  #"://|:|@|/"
                             (s/split db-url)
                             (drop 1)
                             (zipmap [:user :password :host :port :db]))]
    (def connection-map (assoc connection-map :classname "org.postgresql.Driver"
                               :subname (str "//" (:host connection-map) "/" (:db connection-map)
                                             (if-not (= (env :roland) "1")
                                               "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
                                               ""))
                               :subprotocol "postgresql"))
    (kd/defdb im (kd/postgres connection-map))
    (println "Database connection created")
    (init_sql!)
    im)
  (catch Exception ex
    (println "ERROR")
    (pprint ex)
    (throw ex)))

(k/defentity font_preference
  (k/pk :id))

(k/defentity friendship)

(k/defentity users
  (k/pk :username)
  (k/belongs-to font_preference {:fk :font_id})
  (k/has-many friendship {:fk :username1})) ;; I disagree with how korma's has-one/belongs-to works, but whatever

  
(comment
  (k/insert users
            (k/values {:username "feuer" 
                       :displayname "feuer"
                       :password "popsadsadko"
                       :img_location "http://3.bp.blogspot.com/_z3wgxCQrDJY/S6CgYhXSkyI/AAAAAAAAAAg/0Vv0ffa871g/S220-s80/imagex100x100.jpeg"
                       :personal_message "Voikukkia ei voi syödä"
                       :admin true
                       :font_id 1}))
  (use 'improject.security)
  
  (k/insert users
            (k/values {:username "toinen hahmo" 
                       :displayname "toinen hahmo"
                       :password (sha-512 "salis")
                       :img_location "http://4everstatic.com/pictures/80x80/animals/wildlife/red-panda,-head-152734.jpg"
                       :personal_message "Moi maailma"
                       :admin false
                       :can_login false
                       :font_id 1}))

  (k/insert users
            (k/values {:username "kolmas hahmo" 
                       :displayname "kolmas hahmo"
                       :password (sha-512 "salis")
                       :img_location "http://4everstatic.com/pictures/80x80/animals/wildlife/red-panda-152735.jpg"
                       :personal_message "Terve kaikki"
                       :admin false
                       :can_login false
                       :font_id 1}))

  (use 'improject.serialization)

  (make-friends! "feuer" "toinen hahmo")
  (make-friends! "kolmas hahmo" "feuer")
  
  (k/delete users)

  (k/select users
            (k/with friendship))

  (k/select font_preference)

  (k/select friendship)

  (pprint (k/select users
                    (k/with friendship)
                    (k/with font_preference)
                    ;; (k/where (= :username "feuer"))
                    ))

  (k/insert font_preference
            (k/values {:bold true})))
