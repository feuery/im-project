(ns mese-test.util)

(defn in? [seq elm]
  (some #(= % elm) seq))

(defn seq-in-seq? [subseq seq]
  (->> subseq
       (map (partial in? seq))
       (reduce #(and %1 %2))))

(defn to-number
  [Str]
  (cond
   (string? Str) (read-string (re-find #"[0-9.]*" Str))
   (number? Str) Str
   :t (throw (Exception. (str "Unknown type (" (class Str) ") received in to-number")))))

(defn map-to-values [fun map]
  (into {} (for [[k v] map] [k (fun v)])))
