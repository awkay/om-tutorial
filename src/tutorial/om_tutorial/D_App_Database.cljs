(ns om-tutorial.D-App-Database
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defui C
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

(def state-tree {:a 1 :join-b {:b 2 :join-c [{:k 1 :k2 2} {:k 3 :k2 4}]}})

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
  (see [the glossary](#!/om_tutorial.Z_Glossary)).
  
  Remember from the section on Queries that when you co-locate queries on components, you may or may not 
  have interstitial stateless components. This means your UI tree and query tree need not match.
  
  The default database format is structured like the *query tree*. Anything that has a co-located query
  should appear in the application state tree. If the node(s) in the UI tree that correlate with the query
  have an Ident, the the actual state object should be placed in top-level tables, and the state *tree* should
  contain the Ident instead (as a reference to the potentially shared data).
  
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
  then one possible state tree might be:
  "
  state-tree
  "
  and the resulting rendering of this would be:
  
  ")

(defcard rendering-of-A
         (fn [state-atom _]
           (a state-tree)
           )
         state-tree
         {:inspect-data true}
         )

(defcard-doc
  "
  but the overall UI tree has an intermediate (stateless) component between B and C.
  
      A
      |
      B
      |
      I  <- stateless UI. No query.
      |
      C
     /|\\
    D D D
    
  in code this would be:
  
  ## TODO: Diagram of default database format
  
  ## Ident
  
  Components can be given an Ident 
  ")
