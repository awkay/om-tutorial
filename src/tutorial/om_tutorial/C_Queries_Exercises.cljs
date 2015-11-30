(ns om-tutorial.C-Queries-Exercises
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(declare person)

(defui Person
       static om/IQuery
       (query [this] '[:ui/checked :db/id :person/name {:person/mate ...}])
       static om/Ident
       (ident [this {:keys [db/id]}] [:db/id id])

       Object
       (render [this]
               (let [{:keys [ui/checked db/id person/name person/mate]} (om/props this)
                     {:keys [onDelete rendered-mate]} (om/get-computed this)]
                 (dom/li nil
                         (dom/input #js {:type    "checkbox"
                                         ;; Toggle a boolean UI attribute. Must supply the attribute and ref of this
                                         :onClick #(om/transact! this `[(app/toggle-ui-boolean {:attr :ui/checked
                                                                                                :ref  [:db/id ~id]})])
                                         :checked (boolean checked)})
                         name
                         (when onDelete (dom/button #js {:onClick #(onDelete id)} "X"))
                         (when (and mate (not rendered-mate))
                           (dom/ul nil (person (om/computed mate {:rendered-mate true}))))
                         ))))

(def person (om/factory Person {:keyfn :db/id}))

(defui PeopleWidget
       static om/IQuery
       (query [this] `[{:people ~(om/get-query Person)}])
       Object
       (render [this]
               (let [people (-> (om/props this) :people)
                     deletePerson (fn [id] (om/transact! this `[(app/delete-person {:db/id ~id})
                                                                ~(om/force :people :my-server)]))]
                 (dom/div nil
                          (if (= nil people)
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

(def om-root (om/factory Root))

(defcard-doc
  "# Query Exercises

  In this section you learned about queries. In the following exercises we'll continue
  working on our components from the UI Exercises section. The suggested solution to
  the final exercise there is at the top of the source for this namespace.

  ")

(defcard exercise-1
         "## Exercise 1

         OK, you've still got a plain React component. `defui` just makes a React class.

         This exercise is about connecting the dots. The rules for co-located queries are
         summarized here:

         - The interfaces for IQuery etc are declared `static`. This is not a cljs keyword, it is
         an invention of the macro. It causes the macro to place those things on the generated React class.
         - A `defui` component need not have a query. These are known as stateless components.
         - If a component *has* a query, it must be it's own. Do not embed a query of another component as the full query
         of another (you must at least use a join if this is what you want to do).

         Finish building out the suggested goal interface using Om components with co-located queries.

         Remember that you will now have to obtain your properties from `this` using `om/props`.

         This file (just above this card's source) contains starter code. When correct, this card should render
         correctly. Note that this card does not detect the error of using another component's query as
         the full query.
         "
         (fn [state-atom _]
           (om-root @state-atom))
         {:last-error "Some error message"
          :new-person ""
          :widget     {:people [
                                {:db/id 1 :person/name "Joe" :person/mate {:db/id 2 :person/name "Sally"}}
                                {:db/id 2 :person/name "Sally" :person/mate {:db/id 1 :person/name "Joe"}}
                                ]}
          }
         {:inspect-data true}
         )
