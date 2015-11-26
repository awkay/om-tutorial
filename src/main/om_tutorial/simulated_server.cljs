(ns om-tutorial.simulated-server
  (:require
    [om.tempid :as t]
    [datascript.core :as d]
    [om.next :as om]))

;; SIMULATE SERVER ON THE CLIENT
;; Remember that the server uses the same parser code as the client, so we can write it all client-side to play with it!

(def server-state (do
                    (let [schema {:person/mate {:db/cardinality :db.cardinality/one}}
                          conn (d/create-conn schema)]
                      (d/transact conn
                                  [{:db/id 1 :person/name "Sam" :person/mate 2}
                                   {:db/id 2 :person/name "Tammy" :person/mate 1}])
                      conn)))

(defmulti server-read om/dispatch)
(defmethod server-read :people [{:keys [query]} k p]
  {:value
   (d/q '[:find [(pull ?e ?q) ...]
          :in $ ?q
          :where [$ ?e :person/name ?v] ;; all entities with a person/name
          ] (d/db server-state) query)})

(defmethod server-read :default [env k p] :not-found)

(defmulti server-mutate om/dispatch)

(defmethod server-mutate 'app/add-person [{:keys [db state ast] :as env} k {:keys [name]}]
  {
   ; TODO: :tempids { [:db/id tmpid] [:db/id 4]}
   ; Action here is a thunk, because parse MUST be side-effect free.
   :action (fn []
             ; TODO: Server add
             )
   })

(defn is-tempid? [id] (= om.tempid/TempId (type id)))

(def next-id (atom 1000))

;; {:value {:keys … :tempids … :result ...} :action (fn [] ..)}
(defmethod server-mutate 'app/save [{:keys [db state ast] :as env} k params]
  {
   :value  {:keys [:people]}
   :action (fn []
             (let [incoming-items params
                   tempids (keep #(when (is-tempid? (:db/id %)) (:db/id %)) incoming-items)
                   remaps (reduce merge {} (map (fn [tid]
                                                  (swap! next-id inc)
                                                  {[:db/id tid] [:db/id @next-id]}) tempids))
                   perm-id (fn [obj]
                             (if (is-tempid? (:db/id obj))
                               (assoc obj :db/id (second (get remaps [:db/id (:db/id obj)])))
                               obj))
                   remapped-items (mapv perm-id incoming-items)
                   ]
               (swap! server-state assoc :people remapped-items)
               {:tempids remaps}
               ))})


(defmethod server-mutate 'app/delete-person [{:keys [state ast] :as env} k {:keys [db/id name]}]
  {
   :value  {:keys [:people]}
   :action (fn []
             (println "SERVER ASKED TO DELETE" id)
             (d/transact server-state [[:db.fn/retractEntity id]])
             nil
             )
   })

(def server-parser (om/parser {:read server-read :mutate server-mutate}))

(defn pull-up-ids [k v]
  (println "pull " k v)
  (case k
    'app/save (:result v)
    v)
  )
; NOTE: LOTS TO DO STILL...ops, error simulation, etc.
(defn simulated-server
  "A function that simulates the action of the server. The parameter is the incoming data payload (in edn, as if transit was used),
  and the return value is what would be placed in the payload of the response.
  "
  [query]
  (let [resp (server-parser {:state server-state} query)]
      ;  resp' (into {} (map (fn [[k v]] [k (pull-up-ids k v)]) resp))]
    resp))


(comment
  (d/db server-state)
  (d/pull (d/db server-state) [:person/name {:person/mate '...}] 1)
  (d/q '[:find (pull ?e ?q)
         :in $ ?q
         :where [$ ?e :person/name ?v]
         ] (d/db server-state) [:person/name {:person/mate '...}])
  )
