(ns om-tutorial.queries.exercise-util
  (:require [om-tutorial.local-read :as local]
            [om.next :as om]))

(def parser (om/parser {:read local/read-local}))
