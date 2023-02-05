(ns patience.filter)

(defn parse-filters [filter-str]
  (some->> filter-str
           (re-seq
            #"(?::([^\s\"'/]+)/([^\s\"\'/]+)\s([^\s\"']+|\"([^\"]*)\"|'([^']*)'))|([^\s\"']+|\"([^\"]*)\"|'([^']*)')")
           (keep (fn [[_ k p val dq-val q-val like dq-like q-like]]
                   (cond
                     (and k p) [(keyword k) (keyword p) (or q-val dq-val val)]
                     like (or q-like dq-like like))))))
