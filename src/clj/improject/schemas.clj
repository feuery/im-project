(ns improject.schemas
  (:require [schema.core :as s]))

(def user-schema {:personal_message s/Str
                  :color s/Str
                  :underline s/Bool
                  :font_name s/Str
                  :password s/Str
                  :bold s/Bool
                  :username s/Str
                  :italic s/Bool
                  :displayname s/Str
                  :img_location s/Str})

(def sanitized-user-schema (-> user-schema
                               (dissoc :password)))

(def session-user-schema (-> sanitized-user-schema
                             (assoc :sessionid s/Keyword)))
                             

(def login-schema {:username s/Str
                   :password s/Str})

(def message-schema {:message s/Str
                     :sender (-> user-schema
                                 (dissoc :password))
                     ;; todo fix this with cljc files if the need to use these schemas client-side arises
                     :date org.joda.time.DateTime
                     :sent-to [s/Keyword]})

(def enveloped-message-schema {:recipient s/Str
                               :model message-schema})
  
(def inbox-schema {s/Str [message-schema]})

(def session-id-schema {s/Str [s/Keyword]})
