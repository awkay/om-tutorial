(ns om-tutorial.F-Mutation-Exercises
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [om-tutorial.queries.exercise-util :as u]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            [cljs.reader :as r]
            [om.next.impl.parser :as p]))

(defcard-doc
  "# Mutation Exercises

  Plan:
  - Stress application structure using lots of made-up idents
     - Demonstrate how top-level transactions become trivial when everything is in top-level tables
     - Show how many UI-specific mutation concerns can then be combined into a small set of functions

  ")
