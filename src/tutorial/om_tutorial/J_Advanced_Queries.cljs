(ns om-tutorial.J-Advanced-Queries
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc deftest]]
            [cljs.reader :as r]
            [om.next.impl.parser :as p]))

(defcard-doc
  "
  # Dynamic Queries

  Plan:

  - Using parameters in query elements (e.g. joins, props, and calls)
      - Modifying AST elements to rewrite what gets sent in params to a server
  - IQueryParams
  - Dynamically modifying queries and query parameters
  - Embedding query fragments via query parameters

  ")
