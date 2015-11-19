(ns om-tutorial.client-remoting
  (:require [om-tutorial.parsing :as p]
            [om-tutorial.simulated-server :refer [simulated-server]]
            [om.next :as om]))

(defn read-remote
  "The read function used by Om parsing to determine if a remote load should happen for given data.
  
  The helper functions `recurse-remote` and `fetch-if-missing` take a good deal of the complexity out.
  "
  [env key params]
  (case key
    :widget (p/recurse-remote env key)
    :people (p/fetch-if-missing env key :make-root)
    :not-remote ; prune everything else from the parse
    )
  )

(defn send [remote-queries cb]
  (println "REMOTE queries are: " remote-queries)
  (let [payload (:my-server remote-queries)
        {:keys [query rewrite]} (om/process-roots payload) ;; FIXME: BUG: process-roots should NOT return empty!
        server-response (simulated-server query)
        ]
    (println "Suggested query: " payload)
    (println "REMOTE payload (after re-root): " query)
    (js/setTimeout (fn []
                     (println "SERVER response is: " server-response)
                     (cb (rewrite server-response))
                     ) 1000)
    )
  )

