(ns om-tutorial.parsing-cards
  (:require-macros
    [cljs.test :refer [is]]
    [devcards.core :as dc]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [om-tutorial.parsing :as p]
            [om-tutorial.core :as core]
            [om-tutorial.ui :as ui]
            [devcards.core :as dc]
            ))

(let [people-query [:ui.people/checked :db/id :person/name {:people '...}]]
  (dc/deftest strip-ui
              "Strips from query any keyword that has a ui namespace"

              "is-ui-query-fragment? propertly detects if its a ui keywords"
              (is (#'p/is-ui-query-fragment? :ui.people/by-id))
              (is (#'p/is-ui-query-fragment? :ui/by-id))
              (is (not (#'p/is-ui-query-fragment? :my.ui/person)))
              (is (not (#'p/is-ui-query-fragment? :uix/person)))
              (is (not (#'p/is-ui-query-fragment? :new-person)))
              (is (not (#'p/is-ui-query-fragment? "ui/by-id ")))
              (is (not (#'p/is-ui-query-fragment? 36)))
              (is (not (#'p/is-ui-query-fragment? 'ui.foo/bar)))

              "remove-ui-query-fragments removes all ui-query-fragments from a vector"
              (is (= [:db/id :person/name {:people '...}]
                     (#'p/remove-ui-query-fragments people-query)))

              "strip-ui removes all nested ui-query-fragments that are in vectors"
              (is (= [{:widget [{:people (remove #{:ui.people/checked}
                                                 people-query)}]}]
                     (p/strip-ui [{:widget [{:people people-query}]}])))

              "strip-ui removes any properties 'called' with params"
              (is (= [{:widget [{:people []}]}]
                     (p/strip-ui [{:widget [{:people ['(:ui.people/prop {:some :params})]}]}])))
              ))

(let [env {:db-path []}
      env' (p/descend env :a)
      env'' (p/descend env' :b)
      env''' (p/descend env' [:db/id 5])
      path (fn [e] (:db-path e))]
  (dc/deftest descend-tests
              "The descend function tracks which node the parser is on in state by appending the given key to
              :db-path in the given environment.

              The path starts out empty"
              (is (= (path env) []))
              "Adding an element works"
              (is (= (path env') [:a]))
              "Additional elements go on the end"
              (is (= (path env'') [:a :b]))
              "Using a ref replaces the path with a top-level ref."
              (is (= (path env''') [[:db/id 5]]))
              ))

(dc/deftest ui-keys
            "UI Keys"
            " - are derived from ident by pre-pending ui to the namespace of the ident's keyword."
            (is (= [:ui.people/by-id 3] (p/ui-key [:people/by-id 3])))
            )

(dc/deftest parsing-utility-tests
            "follow-ref"
            (let [env {:state (atom {:db/id {1 {:db/id 1 :person/name "Joe"}}})}]
              (is (= {:db/id 1 :person/name "Joe"} (p/follow-ref env [:db/id 1]))))
            )

(let [initial-state {:last-error "Some Error" :new-person "Sally"
                     :widget     {
                                  :people
                                  [{:db/id 1 :person/name "Tony" :garbage 1 :person/mate {:db/id 2 :a 5 :person/name "Jane"}}
                                   {:db/id 2 :person/name "Jane" :garbage 2 :person/mate {:db/id 1 :b 2 :person/name "Tony"}}]
                                  }
                     :ui.db/id   {
                                  2 {:ui.db/id 2 :ui/checked true}
                                  }
                     }
      normalized-state (om/tree->db ui/Root initial-state true)
      env {:state (atom normalized-state)}]
  (dc/deftest local-read-tests
              "Will retrieve last-error"
              (is (=
                    {:last-error "Some Error"}
                    (core/parser env '[:last-error])
                    ))
              "Will retrieve new-person"
              (is (=
                    {:new-person "Sally"}
                    (core/parser env '[:new-person])
                    ))
              "Will retrieve recursive person"
              (is (=
                    {:widget {
                              :people
                              [{:db/id 1 :person/name "Tony" :person/mate {:db/id 2 :person/name "Jane"}}
                               {:db/id 2 :person/name "Jane" :person/mate {:db/id 1 :person/name "Tony"}}]
                              }}
                    (core/parser env '[{:widget [{:people [:db/id :person/name {:person/mate ...}]}]}])
                    ))
              "Can see ui attributes merged into person"
              (is (=
                    {:widget {
                              :people
                              [{:db/id 1 :person/name "Tony" :person/mate {:db/id 2 :ui/checked true :person/name "Jane"}}
                               {:db/id 2 :ui/checked true :person/name "Jane" :person/mate {:db/id 1 :person/name "Tony"}}]
                              }}
                    (core/parser env '[{:widget [{:people [:ui/checked :db/id :person/name {:person/mate ...}]}]}])
                    ))
              "Can do a path optimized read"
              (is (=
                    {:db/id 2 :ui/checked true :person/name "Jane" :person/mate {:db/id 1 :person/name "Tony"}}
                    (get (core/parser env [{[:db/id 2] (om/get-query ui/Person)}]) [:db/id 2])
                    ))
              ))

(defn read [e k p] {:value 42})
(defn mutate [e k p]
  {:value  {:keys    [:a :b]
            :tempids {[:db/id 0] [:db/id 1]}
            :result  {:value-result 1}
            }
   :action (fn []
             {:action-result 2
              :tempids       {[:db/id 999] [:db/id 454]}})
   })
(def mutation-test-parser (om/parser {:read read :mutate mutate}))

;; {:value {:keys … :tempids … :result ...} :action (fn [] ..)}
(let [state (atom {})]
  (dc/deftest mutation-rv
              (is (= [] (mutation-test-parser {:state state} '[(f {:k 3})])))
              (is (= {} @state))

              ))

(defui Thing
       static om/Ident
       (ident [this {:keys [thing-id]}] [:thing thing-id])
       static om/IQuery
       (query [this] [:thing/value]))

(defui Widget
       static om/Ident
       (ident [this {:keys [widget-id]}] [:widget widget-id])
       static om/IQueryParams
       (params [this] {:thing-query {[:thing 9] (om/get-query Thing)}})
       static om/IQuery
       (query [this] [:widget/value '?thing-query]))

(defui Root
       static om/IQuery
       (query [this] [{[:widget 42] (om/get-query Widget)}]))

(defn id-read [{:keys [ast read-path node state parser query] :as env} k p]
  (let [{:keys [key type query]} ast
        path (conj read-path key)]
    (println "READ" read-path)
    (if (om/ident? key)
      {:value (parser (assoc env :read-path path :node (get-in @state key)) query)}
      (do
        (println "value: " (get node key))
        {:value (get node key nil)})
      )


    ))
(def ident-test-parser (om/parser {:read id-read}))

(let [state (atom (om/tree->db Root { [:widget 42] {:widget-id 42 :widget/value 6 [:thing 9] {:thing-id 9 :thing/value 99}}} true))]
  (dc/deftest using-ident
              (is (= [] (om/get-query Root)))
              (is (= [] (ident-test-parser {:state state :read-path [] :node @state} (om/get-query Root))))
              (is (= {} @state))

              ))
