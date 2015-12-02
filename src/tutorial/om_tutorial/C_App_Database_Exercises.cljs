(ns om-tutorial.C-App-Database-Exercises
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            [cljs.reader :as r]))

(defcard-doc
  "
  # App Database Exercises

  TODO:

  - An exercise that shows tree->db cannot normalize without querys on components (idents are needed for that to
  work)
  - An exercise or two using db->tree to fetch data from arbitrary databases (e.g. show normalized database
  and have the user write queries that retrieve various things)

")


