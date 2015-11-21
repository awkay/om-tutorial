(ns om-tutorial.queries.query-demo
  (:require
    [om.next :as om :refer-macros [defui]]
    [om.dom :as dom]))

(defui Person
       static om/IQuery
       (query [this] '[:person/name])
       Object
       (render [this] (let [{:keys [person/name]} (om/props this)] (dom/li nil name))))

(def person (om/factory Person {:keyfn :db/id}))

(defui PeopleWidget
       Object
       (render [this] (let [people (om/props this)] (dom/ul nil (map person people)))))

(def people-list (om/factory PeopleWidget))

(defui Root
       static om/IQuery
       (query [this] `[{:people ~(om/get-query Person)}])
       Object
       (render [this]
               (let [{:keys [people]} (om/props this)] (dom/div nil (people-list people)))))

(def root (om/factory Root))
