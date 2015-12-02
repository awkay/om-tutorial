(ns om-tutorial.client-mutation
  (:require
    [om-tutorial.parsing :as p]
    [om.next :as om]))

(defmulti mutate om/dispatch)

;"Update the client-local holder for an input in progress"
(defmethod mutate 'app/set-new-person
  [{:keys [state] :as env} k {:keys [value]}]
  (println "VALUE: " value)
  {:action (fn [] (swap! state assoc :new-person value))}
  )

(defn ref-to-id [r] (if (om/ident? r) (second r) r))
(defn obj-deref [obj] (into {} (map (fn [[k v]] [k (ref-to-id v)])) obj))

(defmethod mutate 'app/save
  [{:keys [state ast] :as env} k {:keys [name]}]
  ;; rewrite the save params using local state
  (let [save-ast (assoc ast :params {:people (mapv obj-deref (vals (get @state :db/id)))})]
    { :my-server save-ast })
  )

;"Clear the people, which will trigger a re-read from remote."
(defmethod mutate 'app/refresh
  [{:keys [state ast] :as env} k {:keys [name]}]
  {:action (fn []
             (swap! state assoc-in [:widget :people] nil)
             )}
  )

;"Add a person. Does this locally only, with temporary IDs. These are persisted on save."
(defmethod mutate 'app/add-person
  [{:keys [state ast] :as env} k {:keys [name]}]
  {
   :action (fn []
             ;; I can play with normalized tables, or use db->tree and tree->db.
             (let [temp-id (om/tempid)
                   create-or-conj (fn [possible-collection value] (if (vector? possible-collection)
                                                                    (conj possible-collection value)
                                                                    [value]
                                                                    ))]
               (swap! state assoc :new-person "")
               (swap! state update-in [:db/id] assoc temp-id {:db/id temp-id :person/name name})
               (swap! state update-in [:widget :people] create-or-conj [:db/id temp-id]))
             )
   })

(defn remove-person
  "Remove a person helper function"
  [id current-ppl-refs] (filterv #(not= id (second %)) current-ppl-refs))

(defmethod mutate 'app/delete-person
  [{:keys [state ast] :as env} k {:keys [db/id]}]
  (println "delete " id)
  {:my-server ast
   :action    (fn []
               ; (swap! state update-in [:widget :people] (partial remove-person id))
                )
   })

(defmethod mutate 'app/toggle-person-checkbox
  [{:keys [state]} k {:keys [db/id]}]
  {:action (fn []
             (let [path [:db/id id :ui/checked]]
               (swap! state update-in path not))
             )}
  )

