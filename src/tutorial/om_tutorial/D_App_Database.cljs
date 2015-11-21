(ns om-tutorial.D-App-Database
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defui C
       static om/Ident
       (ident [this props] [:c/by-k (:k props)])
       static om/IQuery
       (query [this] [:k :k2])
       Object
       (render [this] (let [{:keys [k k2]} (om/props this)] (dom/div nil (str "C: " k ", " k2)))))

(def c (om/factory C))

(defui I
       Object
       (render [this]
               (dom/div nil
                        (dom/p nil "I: no state...just extra UI cruft")
                        (c (om/props this)))))              ; just pass the om props on through

(def i (om/factory I))

(defui B
       static om/IQuery
       (query [this] [:b {:join-c (om/get-query C)}])
       Object
       (render [this] (let [{:keys [b join-c]} (om/props this)]
                        (dom/div nil (str "B: " b)
                                 (map i join-c)             ; pass the picked data to interstitial
                                 ))))

(def b (om/factory B))

(defui A
       static om/IQuery
       (query [this] [:a {:join-b (om/get-query B)}])
       Object
       (render [this] (let [{:keys [a join-b]} (om/props this)]
                        (dom/div nil (str "A: " a)
                                 (b join-b)
                                 ))))

(def a (om/factory A))

(def result-tree {:a 1 :join-b {:b 2 :join-c [{:k 1 :k2 2} {:k 3 :k2 4}]}})

(defcard-doc
  "
  # App Database
  
  The client application database (client local state) is fully pluggable in Om; however, Om provides support for
  a default database format, and understanding how to work with that will not only help you understand the 
  common use-cases with standard Om, but also what you would have to do in order to plug in an alternate database
  in the client.
  
  ## The Default Format
  
  The default database format is an edn map. It can have as many top-level keys as make sense to you, but in general
  there will be top-level keys for each property in the top-level query of your UI. For Om to work right, anything
  that might be shared in the UI (or is dealt with in persistence layers) should have an ident 
  (see [the glossary](#!/om_tutorial.Z_Glossary)). We'll get to that in a minute.
  
  Remember from the section on Queries that when you co-locate queries on components, you may or may not 
  have interstitial stateless components. 
  The default database format is structured like the *query tree*. Anything that has a co-located query
  should appear in the application state tree. 
  This means your UI tree and query tree almost certainly will not match.
  
  For example, if you had the following UI elements:
  
  "
  (dc/mkdn-pprint-source C)
  (dc/mkdn-pprint-source c)
  (dc/mkdn-pprint-source I)
  (dc/mkdn-pprint-source i)
  (dc/mkdn-pprint-source B)
  (dc/mkdn-pprint-source b)
  (dc/mkdn-pprint-source A)
  (dc/mkdn-pprint-source a)
  "
  
  you'd end up with this as the query: `(om/get-query A)`
  "
  (om/get-query A)
  "
  then one possible *result* tree for this UI might be:
  "
  result-tree
  "
  and the resulting rendering of this would be:
  ")

(defcard rendering-of-A
         (fn [state-atom _]
           (a result-tree)
           )
         result-tree
         {:inspect-data true}
         )

(defcard-doc
  "
  So while the overall UI tree has `I` in it, the query result tree does not (since `I` has no query).
  
  But we're not done yet. What if two components on the screen happen to render different views of the same 
  data? Any time you pull information from a server, it is possible someone might co-locate a query for it, and
  then you're screwed...updating one branch of the UI state tree will only cause one component to update.
  
  Identity (and cross-referencing) to the rescue.
  
  By placing an Ident on your component, you enable Om to understand when two components are rendering data derived
  from the same source (e.g. a table and a graph).
  
  In order for this to work well, you ideally want to have that shared data in your app state just once.
  
  An ident is just a unique identity, represented as a 2-tuple `vector` with a first element keyword. An ident need only
  be client-unique, but will often be based on real server-persisted data. Examples might be `[:people/by-id 3]`
  and `[:ui.button/by-id 42]`. Om can use these to find components that share state and should update together,
  and for other things like parse optimization.
  
  So, the default Om database format requires that any component with an Ident have it's state represented in the 
  state database as the ident of the object, and the actual data for that object goes at the *top* of the state
  map, as a database map of those kind of objects. The Om function `tree->db` can convert such database *state* 
  trees into this format given a query from components that have `Ident`. 
  
  If we take the proposed *result tree*:"
  result-tree
  "
  and apply `(om/tree->db A result-tree true)`:"
  (om/tree->db A result-tree true)
  "
  
  Then we arrive at the desired default database format for Om.
  
  Formally, this is a tree of data where all of the objects that have an ident are replaced by that ident, and 
  the actual data of those objects is moved to top-level Om-owned tables.
  
  ## TODO: Diagram of default database format
  
  ")
