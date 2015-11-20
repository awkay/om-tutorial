(ns om-tutorial.local-read
  (:require [om-tutorial.parsing :as p]
            [om.next :as om]))

(defn read-local
  "The function used by Om parser to read local app state."
  [env key params]
  (case key
    :person/mate {:value (p/parse-join-with-reader read-local env key :limit 2)}
    :people {:value (p/parse-join-with-reader read-local env key)}
    :widget {:value (p/parse-join-with-reader read-local env key :reset-depth 0)}
    (p/db-value env key)
    ))

(defn merge-tree [state result]
  (println "Asked to merge tree " result " into " state)
  (let [ppl (-> result :widget :people)]
    (cond-> state
            ppl (assoc :people ppl)
            :always (om/default-merge-tree result)
            )))
