(ns om-tutorial.I-Path-Optimization
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
  ## Path Optimization

  If your UI gets rather large, you may see warnings in the Javascript Console of the browser about slowness. If you do,
  you can leverage path optimization to minimize the amount of work the parser has to do in order to update a sub-portion
  of the UI.

  If you pass `:pathopt true` to the reconciler, then when re-rendering a component that has an Ident
  Om will attempt to run the query starting from that component (using it's Ident as the root of the query).
  If you parser returns a result, it will use it. If your parser returns nil then it will focus the root
  query to that component and run it from root.

  When it attempts this kind of read it will call your `read` function with `:query/root` set to the ident of the component that
  is needing re-render, and you will need to follow the query down from there. Fortunately, `db->tree` still works
  for the default database format with a little care.

  ")

