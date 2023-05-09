(ns patience.filter
  (:require [clojure.string :as string]))

(defn parse-filters [filter-str]
  (some->> filter-str
           (re-seq
            #"(?::([^\s\"'/]+)/([^\s\"\'/]+)\s([^\s\"']+|\"([^\"]*)\"|'([^']*)'))|([^\s\"']+|\"([^\"]*)\"|'([^']*)')")
           (keep (fn [[_ k p val dq-val q-val like dq-like q-like]]
                   (cond
                     (and k p) [(keyword k) (keyword p) (or q-val dq-val val)]
                     like (or q-like dq-like like))))
           (map (fn [filter]
                  (let [[k p v] filter]
                    (if (= p :between)
                      [k p (string/split v #", ")]
                      filter))))))
