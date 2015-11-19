(ns om-tutorial.simulated-server
  (:require [om.next :as om]))

;; SIMULATE SERVER ON THE CLIENT
;; Remember that the server uses the same parser code as the client, so we can write it all client-side to play with it!

(defonce server-state (atom {:people [{:db/id 1 :person/name "Sam"} {:db/id 2 :person/name "Tammy"}]}))

(defmulti server-read om/dispatch)
(defmulti server-mutate om/dispatch)

(defmethod server-mutate 'app/add-person [{:keys [state ast] :as env} k {:keys [name]}]
  {
   ; Action here is a thunk, because parse MUST be side-effect free.
   :action (fn []
             ; TODO: Server add
             )
   })

(defn simulated-server
  "A function that simulates the action of the server. The parameter is the incoming data payload (in edn, as if transit was used),
  and the return value is what would be placed in the payload of the response.
  "
  [query]
  )

(defn send [remote-queries cb]
  (println "REMOTE queries are: " remote-queries)
  (let [payload (:my-server remote-queries)
        server-response (simulated-server payload)
        {:keys [query rewrite]} (om/process-roots payload)
        ]
    (println "Suggested query: " payload)
    (println "REMOTE payload (after re-root): " query)
    (js/setTimeout (fn []
                     (println "SERVER response is: " server-response)
                     ) 1000)
    )
  )


