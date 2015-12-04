(ns om-tutorial.simulated-server
  (:require
    [om.tempid :as t]
    [datascript.core :as d]
    [om.next :as om]))

;; SIMULATE SERVER ON THE CLIENT
;; Remember that the server uses the same parser code as the client, so we can write it all client-side to play with it!

;; Pretend we're using Datomic, and have some initial data
(def server-state (do
                    (let [schema {:person/mate {:db/cardinality :db.cardinality/one}}
                          conn (d/create-conn schema)]
                      (d/transact conn
                                  [{:db/id 1 :person/name "Sam" :person/mate 2 :n/a 2}
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

(defn is-tempid? [id] (= om.tempid/TempId (type id)))

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
  {:value  {:keys [:people]}
   :action #(save-people (:people params))})

(defmethod server-mutate 'app/delete-person [{:keys [state ast] :as env} k {:keys [db/id name]}]
  {
   :value  {:keys [:people]}
   :action (fn []
             ;(println "SERVER ASKED TO DELETE" id)
             (d/transact server-state [[:db.fn/retractEntity id]])
             nil
             )
   })

(def server-parser (om/parser {:read server-read :mutate server-mutate}))

(defn pull-up-ids
  "Return values of functions come back in result. Pulls the id remaps up a level."
  [k v]
  (case k
    'app/save (-> v
                  (merge (:result v))
                  (dissoc :result))
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

