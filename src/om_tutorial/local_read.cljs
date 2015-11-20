(ns om-tutorial.local-read
  (:require [om-tutorial.parsing :as p]
            [om.next :as om]))

(defn read-person
  [{:keys [query] :as env} key params]
  (println "read person " key " q:" query)
  (case key
    :person/mate {:value (p/parse-join-with-reader read-person env key :limit 2)}
    (p/db-value env key)
    )
  )

(defn read-widget
  [{:keys [state] :as env} key params]
  (case key
    :people {:value (p/parse-join-with-reader read-person env key)}
    (p/db-value env key)
    )
  )

(defn read-local
  "The function used by Om parser to read local app state."
  [env key params]
  (case key
    :widget {:value (p/parse-with-reader read-widget env key true)}
    (p/db-value env key)
    )
  )

(defn merge-tree [state result]
  (println "Asked to merge tree " result " into " state)
  (let [ppl (-> result :widget :people)]
    (cond-> state
            ppl (assoc :people ppl)
            :always (om/default-merge-tree result)
            )
    )
  )

