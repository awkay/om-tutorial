(ns om-tutorial.local-read
  (:require [om-tutorial.parsing :as p]
            [om.next :as om]))

(defn read-person [env key params]
  (case key
    :person/mate {:value (p/parse-join-with-reader read-person env key :limit 2)}
    (p/db-value env key)
    ))

(defn read-widget [env key params]
  (case key
    :people {:value (p/parse-join-with-reader read-person env key)}
    (p/db-value env key)
    ))

(defn read-local
  "The function used by Om parser to read local app state."
  [env key params]
  (case key
    :widget {:value (p/parse-join-with-reader read-widget env key :reset-depth 0)}
    (p/db-value env key)
    ))

(defn merge-tree [state result]
  (println "Asked to merge tree " result " into " state)
  (let [ppl (-> result :widget :people)]
    (cond-> state
            ppl (assoc :people ppl)
            :always (om/default-merge-tree result)
            )))
