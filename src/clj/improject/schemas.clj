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

(def sanitized-user-schema (dissoc user-schema :password))

(def login-schema {:username s/Str
                   :password s/Str})

(def message-schema {:recipient s/Str
                     :model {:message s/Str
                             :sender (-> user-schema
                                         (dissoc :password))}})
  
