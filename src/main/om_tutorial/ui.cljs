(ns om-tutorial.ui
  (:require
    [om.next :as om :refer-macros [defui]]
    [om.dom :as dom]))

(declare person)

(defui Person
       static om/IQuery
       (query [this] '[:ui.people/checked :db/id :person/name {:person/mate ...}])
       static om/Ident
       (ident [this {:keys [db/id]}] [:people/by-id id])

       Object
       (render [this]
               (let [{:keys [ui.people/checked db/id person/name person/mate]} (om/props this)
                     {:keys [onDelete rendered-mate]} (om/get-computed this)]
                 (dom/li nil
                         (println "re-rendering " name)
                         (dom/input #js {:type    "checkbox"
                                         ;; Toggle a boolean UI attribute. Must supply the attribute and ref of this 
                                         :onClick #(om/transact! this `[(app/toggle-ui-boolean {:attr :ui.people/checked
                                                                                                :ref  [:people/by-id ~id]}) 
                                                                        [:people/by-id ~(:db/id mate)]
                                                                        ])
                                         :checked (boolean checked)})
                         name
                         (when onDelete (dom/button #js {:onClick #(onDelete id)} "X"))
                         (when (and mate (not rendered-mate)) (dom/ul nil (person (om/computed mate {:rendered-mate true}))))
                         ))))

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
                                     (dom/button #js {:onClick #(om/transact! this '[(app/refresh) :people])} "Refresh List")
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

