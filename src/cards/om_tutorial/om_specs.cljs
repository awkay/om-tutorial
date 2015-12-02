(ns om-tutorial.om-specs
  (:require-macros
    [cljs.test :refer [is]]
    [devcards.core :as dc]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [goog.dom :as gdom]
            [om.dom :as dom]
            [om-tutorial.core :as core]
            [om-tutorial.ui :as ui]
            [devcards.core :as dc]
            ))

(dc/deftest tempid-merge-test
            "Simulate through entire reconciler a server response for an app save that rewrites
            temporary IDs."
            (let [tmpid (om/tempid)
                  new-id 42
                  ;; in tree format:
                  starting-state {:widget {:people [{:db/id tmpid :person/name "Joe"}]}}
                  ;; in db format:
                  expected-state {:widget {:people [[:db/id new-id]]}
                                  :db/id  {42 {:db/id 42 :person/name "Joe"}}
                                  }
                  server-response {'app/save {:tempids {[:db/id tmpid] [:db/id 42]}}}
                  r (om/reconciler {:parser core/parser
                                    :id-key :db/id
                                    :state  starting-state})
                  ;; the cards.html file has a fake (hidden) div so we can integration test this:
                  _ (om/add-root! r ui/Root (gdom/getElement "fake"))]
              ;; pretend we got an app/save back from the server...Om calls merge! on each result from server
              (om/merge! r server-response)
              (is (= expected-state (select-keys (-> r :config :state deref) [:widget :db/id])))
              ))

