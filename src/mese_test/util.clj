(ns mese-test.util)

(defn in? [seq elm]
  (some #(= % elm) seq))
