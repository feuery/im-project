(ns mese-test.util)

(defn in? [seq elm]
  (some #(= % elm) seq))

(defn to-number
  [Str]
  (cond
   (string? Str) (read-string (re-find #"[0-9.]*" Str))
   (number? Str) Str
   :t (throw (Exception. (str "Unknown type (" (class Str) ") received in to-number")))))
