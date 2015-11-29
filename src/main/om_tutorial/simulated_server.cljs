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
          :where [$ ?e :person/name ?v]                     ;; all entities with a person/name
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

(defn save-people [people]
  (let [all-ids (map :db/id people)
        clientid-map (reduce merge {} (map (fn [id] (if (is-tempid? id)
                                                      {id (d/tempid :db.part/user)}
                                                      {id id}
                                                      )) all-ids))
        client-tempids->dscript-tempids (into {} (filter #(is-tempid? (first %)) clientid-map))
        transaction (mapv (fn [person] (assoc person :db/id (get clientid-map (:db/id person)))) people)
        dscript-tempid->real-id (:tempids @(d/transact server-state transaction))
        remaps (reduce
                 (fn [acc [cid dtmpid]]
                   (assoc acc [:db/id cid] [:db/id (dscript-tempid->real-id dtmpid)]))
                 {}
                 client-tempids->dscript-tempids)
        ]
    {:tempids remaps}))

;; {:value {:keys … :tempids … :result ...} :action (fn [] ..)}
(defmethod server-mutate 'app/save [{:keys [db state ast] :as env} k params]
  (println "Save params: " params)
  {:value  {:keys [:people]}
   :action #(save-people (:people params))})

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
  (let [resp (server-parser {:state server-state} query)
    resp' (into {} (map (fn [[k v]] [k (pull-up-ids k v)]) resp))]
    resp'))


(comment
  (d/tempid :db.part/user)
  (d/db server-state)
  (d/pull (d/db server-state) [:person/name {:person/mate '...}] 1)
  (d/q '[:find (pull ?e ?q)
         :in $ ?q
         :where [$ ?e :person/name ?v]
         ] (d/db server-state) [:person/name {:person/mate '...}])
  )

