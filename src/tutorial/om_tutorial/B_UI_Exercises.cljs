(ns om-tutorial.B-UI-Exercises
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "# UI Exercises

  In this tutorial we are going to build an app with just enough complexity to
  exercise the most significant features of Om. That said, we want it to be
  tractable.

  We're going to first build a UI that can show a list of people and their
  partners. To that, we're going to add a spot to show error messages,
  controls for adding a new person,
  saving the list, and requesting a refresh from the server. The overall UI
  should look like the card below (which is build with plain React dom elements
  and function composition):
  "
  )

(defn person [{:keys [person/name person/mate]}]
  (dom/li nil
          (dom/input #js {:type "checkbox"})
          name
          (dom/button nil "X")
          (when mate (dom/ul nil (person mate)))
          ))

(defn people-list [people]
  (dom/div nil
           (dom/button nil "Save")
           (dom/button nil "Refresh List")
           (dom/ul nil (map #(person %) people))))

(defn root [state-atom]
  (let [{:keys [last-error people new-person] :as ui-data} @state-atom]
    (dom/div nil
             (dom/div nil (when (not= "" last-error) (str "Error " last-error)))
             (dom/div nil
                      (dom/div nil
                               (if (= nil people)
                                 (dom/span nil "Loading...")
                                 (people-list people)

                                 )
                               (dom/input {:type "text" :value new-person})
                               (dom/button nil "Add Person")))
             )))

(defcard overall-goal
         (fn [state-atom _]
           (root state-atom))
         {:last-error "Some error message"
          :new-person ""
          :people     [
                       {:db/id 1 :person/name "Joe" :person/mate {:db/id 2 :person/name "Sally"}}
                       {:db/id 2 :person/name "Sally" :person/mate {:db/id 1 :person/name "Joe"}}
                       ]
          }
         {:inspect-data true}
         )

(declare om-person)

(defui Person
       Object
       (render [this]
               (let [name "name" ;; TODO: Get the Om properties from this
                     mate nil]
                 (dom/li nil
                        (dom/input #js {:type "checkbox"})
                        name
                        (dom/button nil "X")
                        (when mate (dom/ul nil (om-person mate)))
                        )))
       )

(def om-person (om/factory Person))

(defcard exercise-1
         "## Exercise 1

         Create an Om Person UI component. No need to add a query yet.

         The template is in
         this tutorial file. You've got it right when the following card renders a person and their mate:
         "
         (fn [state-atom _]
           (om-person @state-atom))
         {:db/id 1 :person/name "Joe" :person/mate {:db/id 2 :person/name "Sally"}}
         {:inspect-data true}
         )

(defui PeopleWidget
       Object
       (render [this]
               (let [people []] ; TODO: Get yo stuff
                 (dom/div nil
                          (if (= nil people)
                            (dom/span nil "Loading...")
                            (dom/div nil
                                     (dom/button #js {} "Save")
                                     (dom/button #js {} "Refresh List")
                                     (dom/ul nil (map #(person %) people))))
                          )
                 )
               )
       )

(def people-widget (om/factory PeopleWidget))

(defui Root
       Object
       (render [this]
               (let [{:keys [widget new-person last-error]} {}] ; TODO: Get yo stuff
                 (dom/div nil
                          (dom/div nil (when (not= "" last-error) (str "Error " last-error)))
                          (dom/div nil
                                   (people-widget widget)
                                   (dom/input #js {:type "text" })
                                   (dom/button #js {} "Add Person")))
                 )))

(def om-root (om/factory Root))

(defcard exercise-2
         "## Exercise 2

         Continue and build out two more components as seen in the source just above this file.

         NOTE: If you look in the
         data below, you'll see our desired UI tree in data form. Use `om/props` to pull out the
         correct pieces at each level of the rendered UI. When you do this correctly, the
         card should render properly.
         "
         (fn [state-atom _]
           (om-root @state-atom))
         {:last-error "Some error message"
          :new-person "something typed by the user"
          :widget     {:people [
                                {:db/id 1 :person/name "Joe" :person/mate {:db/id 2 :person/name "Sally"}}
                                {:db/id 2 :person/name "Sally" :person/mate {:db/id 1 :person/name "Joe"}}
                                ]}
          }
         {:inspect-data true}
         )

(defcard exercise-3
        "
        ## Exercise 3 - Component local state

        TODO...This exercise is not complete. Basically, hook up some event handlers with component
        local state.

        Add component local state so that the following UI elements :

        - Checkboxes on a person
        - The value of the \"new person\" box.
        "
         (fn [state-atom _]
           (om-root @state-atom))
         {:last-error "Some error message"
          :new-person "something typed by the user"
          :widget     {:people [
                                {:db/id 1 :person/name "Joe" :person/mate {:db/id 2 :person/name "Sally"}}
                                {:db/id 2 :person/name "Sally" :person/mate {:db/id 1 :person/name "Joe"}}
                                ]}
          }
         {:inspect-data true}
         )
