(ns om-tutorial.ui
  (:require 
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui Person
       static om/IQuery
       (query [this] '[:db/id :person/name {:person/mate ...}])
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

