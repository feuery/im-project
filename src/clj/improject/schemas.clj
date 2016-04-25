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

(def login-schema {:username s/Str
                   :password s/Str})

