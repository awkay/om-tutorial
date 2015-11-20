(ns om-tutorial.simulated-server
  (:require [om.next :as om]))

;; SIMULATE SERVER ON THE CLIENT
;; Remember that the server uses the same parser code as the client, so we can write it all client-side to play with it!

(defonce server-state (atom {:people [{:db/id 1 :person/name "Sam"} {:db/id 2 :person/name "Tammy"}]}))

(defmulti server-read om/dispatch)
(defmethod server-read :people [{:keys [state]} k p] {:value (-> @state k)})
(defmethod server-read :default [env k p] :not-found)

(defmulti server-mutate om/dispatch)

(defmethod server-mutate 'app/add-person [{:keys [db state ast] :as env} k {:keys [name]}]
  {
   :tempids { [:db/id tmpid] [:db/id 4]}
   ; Action here is a thunk, because parse MUST be side-effect free.
   :action (fn []
             ; TODO: Server add
             )
   })

(defmethod server-mutate 'app/delete-person [{:keys [state ast] :as env} k {:keys [name]}]
  {
   ; Action here is a thunk, because parse MUST be side-effect free.
   :action (fn []
             (println "SERVER ASKED TO DELETE")
             )
   })

(def server-parser (om/parser {:read server-read :mutate server-mutate}))

(defn simulated-server
  "A function that simulates the action of the server. The parameter is the incoming data payload (in edn, as if transit was used),
  and the return value is what would be placed in the payload of the response.
  "
  [query]
  (server-parser {:state server-state} query)
  )


