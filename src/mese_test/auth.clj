(ns mese-test.auth
  (:require [mese-test.user :refer [create-user
                                    get-outbox!
                                    get-inbox!]]
            [com.ashafa.clutch :as c]
            [clojure.pprint :refer :all]
            [mese-test.util :refer [in?
                                    to-number]]))


(defn sha-512 [data]
  (let [md (. java.security.MessageDigest getInstance "sha-512")]
    (. md update (.getBytes data))
    (let [bytes (. md digest)]
      (reduce #(str %1 (format "%02x" %2)) "" bytes))))



                         
      





