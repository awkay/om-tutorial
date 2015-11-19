(ns om-tutorial.client-mutation
  (:require [om.next :as om]))

(defmulti mutate om/dispatch)
(defmethod mutate 'app/set-new-person [{:keys [state] :as env} k {:keys [value]}]
  {:action (fn [] (swap! state assoc :new-person value))}
  )
(defmethod mutate 'app/refresh [{:keys [state ast] :as env} k {:keys [name]}]
  {:action (fn []
             (swap! state assoc :people :missing)
             )}
  )
(defmethod mutate 'app/add-person [{:keys [state ast] :as env} k {:keys [name]}]
  {
   :my-server ast
   ; Action here is an optimistic update. If you have send in "deny add" simulation mode, you'll temporarily see the change
   :action    (fn []
                ;; I can play with normalized tables, or use db->tree and tree->db.
                (let [temp-id (om/tempid)
                      create-or-conj (fn [possible-collection value] (if (vector? possible-collection)
                                                                       (conj possible-collection value)
                                                                       [value]
                                                                       ))]
                  (println "Optimistic add " name " w/tempid " temp-id)
                  (swap! state assoc :new-person "")
                  (swap! state update-in [:db/id] assoc temp-id {:db/id temp-id :person/name name})
                  (swap! state update-in [:people] create-or-conj [:db/id temp-id]))
                )
   })

(defn remove-person [id current-ppl-refs] (filterv #(not= id (second %)) current-ppl-refs))

(defmethod mutate 'app/delete-person [{:keys [state ast] :as env} k {:keys [db/id]}]
  {:my-server ast
   :action    (fn []
                (println "Optimistic delete of " id " " (remove-person id (-> @state :people)))
                (swap! state update-in [:people] (partial remove-person id))
                )
   })

