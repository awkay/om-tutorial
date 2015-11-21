(ns om-tutorial.Z-Glossary
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "# Glossary of Terms
  
  - `Ident`: A unique identity, represented as a 2-tuple `vector` with a first element keyword. An ident need only
    be client unique, but will often be based on real server-persisted data. Examples might be `[:people/by-id 3]`
    and `[:ui.button/by-id 42]`. Om can use these to find components that share state and should update together,
    and for other things like parse optimization.
  ")
