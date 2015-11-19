(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om-tutorial.parsing :as p]
            [om-tutorial.local-read :as local]
            [om-tutorial.remote-fetch :as remote]
            [om-tutorial.simulated-server :as server]
            [om.dom :as dom]))

(enable-console-print!)

(def initial-state {:last-error "" :new-person "" :people :missing})

(defui Person
       static om/IQuery
       (query [this] [:db/id :person/name])
       static om/Ident
       (ident [this {:keys [db/id]}] [:db/id id])

       Object
       (render [this]
               (let [{:keys [db/id person/name]} (om/props this)
                     {:keys [onDelete]} (om/get-computed this)]
                 (dom/li nil name (dom/button #js {:onClick #(when onDelete (onDelete id))} "X")))))

(def person (om/factory Person {:keyfn :db/id}))

(defui PeopleWidget
       static om/IQuery
       (query [this] `[{:people ~(om/get-query Person)}])
       Object
       (render [this]
               (let [people (-> (om/props this) :people)
                     deletePerson (fn [id] (om/transact! this `[(app/delete-person {:db/id ~id}) :people]))]
                 (dom/div nil
                          (if (= :missing people)
                            (dom/span nil "Loading...")
                            (dom/div nil
                                     (dom/button #js {:onClick #(om/transact! this '[(app/save)])} "Save")
                                     (dom/button #js {:onClick #(om/transact! this '[(app/refresh)])} "Refresh List")
                                     (dom/ul nil (map #(person (om/computed % {:onDelete deletePerson})) people))))
                          )
                 )
               )
       )

(def people-list (om/factory PeopleWidget))

(defui Root
       static om/IQuery
       (query [this] `[:new-person :last-error {:widget ~(om/get-query PeopleWidget)}])
       Object
       (render [this]
               (let [setInputValue (fn [e] (om/transact! this `[(app/set-new-person {:value ~(.. e -target -value)})]))
                     addPerson (fn [name] (om/transact! this `[(app/add-person {:name ~name}) :people]))
                     {:keys [widget new-person last-error]} (om/props this)]
                 (dom/div nil
                          (dom/div nil (when (not= "" last-error) (str "Error " last-error)))
                          (dom/div nil
                                   (people-list widget)
                                   (dom/input #js {:type "text" :value new-person :onChange setInputValue})
                                   (dom/button #js {:onClick #(addPerson new-person)} "Add Person")))
                 )))

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


(defn read-entry-point
  [{:keys [target reader] :as env} key params]
  (cond
    reader (reader env key params)
    (= :my-server target) (remote/read-remote env key params)
    :else (local/read-local env key params)
    ))

(def parser (om/parser {:read read-entry-point :mutate mutate}))

(def reconciler (om/reconciler {:state   initial-state
                                :parser  parser
                                :remotes [:my-server]
                                :send    server/send}))

(om/add-root! reconciler Root (gdom/getElement "app"))


