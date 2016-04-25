(ns improject.formtools)

(defn value-of [event]
  (-> event .-target .-value))

(defn value-of-checkbox [event]
  (-> event .-target .-checked))

(defn return-clicked? [e]
  (= (.-which e) 13))
