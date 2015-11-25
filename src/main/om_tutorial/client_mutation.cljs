(ns om-tutorial.client-mutation
  (:require
    [om-tutorial.parsing :as p]
    [om.next :as om]))

(defmulti mutate om/dispatch)

;"Update the client-local holder for an input in progress"
(defmethod mutate 'app/set-new-person
  [{:keys [state] :as env} k {:keys [value]}]
  {:action (fn [] (swap! state assoc :new-person value))}
  )

(defmethod mutate 'app/save
  [{:keys [state ast] :as env} k {:keys [name]}]
  (println "Locale Mutate " ast)
  (let [save-ast (assoc ast :params (get @state :db/id))]
    {
     :my-server save-ast
     })
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
               (println "Optimistic add " name " w/tempid " temp-id)
               (swap! state assoc :new-person "")
               (swap! state update-in [:db/id] assoc temp-id {:db/id temp-id :person/name name})
               (swap! state update-in [:widget :people] create-or-conj [:db/id temp-id]))
             )
   })

(defn remove-person
  "Remove a person helper function"
  [id current-ppl-refs] (filterv #(not= id (second %)) current-ppl-refs))

#_"Delete a person. Does an optimistic delete, and makes a request to the server.

We can do several possible things to deal with server response:
- If error, we detect that in send
   - could add a message to the UI/trigger re-read of all ppl.
   - could keep track of the delete, and undo it (w/UI message)
   - server error could include new state???
"
(defmethod mutate 'app/delete-person
  [{:keys [state ast] :as env} k {:keys [db/id]}]
  {:my-server ast
   :action    (fn []
                (println "Optimistic delete of " id " " (remove-person id (-> @state :people)))
                (swap! state update-in [:widget :people] (partial remove-person id))
                )
   })

#_"Handles a generalized bit of UI state that represents a UI boolean.
Parameter should be the `ref` of the UI component that owns the boolean."
(defmethod mutate 'app/toggle-ui-boolean
  [{:keys [state]} k {:keys [ref attr]}]
  {:action (fn []
             (let [path (conj (p/ui-key ref) attr)]
               (swap! state update-in path not))
             )}
  )

