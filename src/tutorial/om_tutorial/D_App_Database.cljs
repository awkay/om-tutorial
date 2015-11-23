(ns om-tutorial.D-App-Database
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            [cljs.reader :as r]))

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

  ## Ident

  But we're not done yet. What if two components on the screen happen to render different views of the same
  data? Any time you pull information from a server, it is possible someone might co-locate a query for it, and
  then you're screwed...updating one branch of the UI state tree will only cause one component to update.
  
  In order for this to work well, you ideally want to have that shared data in your app state just once.

  So, the default Om database format encourages you to identify this kind of data and put the actual
  data in top-level tables. Then, in the UI state tree, place references (idents) to that top-level data. This
  way, you can update it in one place, and Om can re-render everything that depends on it.

  An ident is just a unique identity, represented as a 2-tuple `vector` with a first element keyword. An ident need only
  be client-unique, but will often be based on real server-persisted data. Examples might be `[:people/by-id 3]`
  and `[:ui.button/by-id 42]`.

  Ident is declared a lot like queries, but in this case you
  will be passed the props of the component (e.g. when it mounts) so the ident function
  can return a specific identity for the component that is actually mounted in the DOM:

  ```
  (defui Component
    static om/Ident
    (ident [this props] [:c/by-k (:k props)])
    ...)
  ```

  ## Sample Default Database


  The Om function `tree->db` can convert non-normalized state
  trees into this format given a query that includes components that have `Ident`.

  If we take a proposed *result tree* and apply `(om/tree->db A result-tree true)`,
  then we'll get a database in the default format. Formally, this is a tree of data
  where all of the objects that *have* an ident are replaced *by* that ident, and
  the actual data of those objects is moved to top-level Om-owned tables.
  ")

(defn convert [s]
  (try
    (om/tree->db A (r/read-string s) true)
    (catch js/Error e (str "Invalid input. Make sure you gave valid edn. Reload the page to start over."))
    ))

(defcard
  (fn [state-atom _]
    (let [{:keys [v]} @state-atom]
      (dom/div nil
               (dom/input #js {:size     100 :type "text" :value v
                               :onChange (fn [evt] (swap! state-atom assoc :v (.. evt -target -value)))})
               (dom/button #js {:onClick #(swap! state-atom assoc
                                                 :normalized-database (convert v))} "Update")
               )))
  {:v                   (str result-tree)
   :normalized-database (om/tree->db A result-tree true)}
  {:inspect-data true})

(defcard-doc
  "
  Feel free to play with the above dev card. I recommend trying:

  - Try changing element IDs in the `:join-c` vector.
  - Add another map to the vector under `:join-c`
  - Remove a `:k` entry from one of the items in the vector. What happens?

## TODO: Diagram of default database format

")
