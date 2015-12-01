(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om-tutorial.parsing :as p]
            [om-tutorial.ui :as ui]
            [om-tutorial.client-mutation :as m]
            [om-tutorial.local-read :as local]
            [om-tutorial.client-remoting :as remote]
            [om-tutorial.simulated-server :as server]
            [cljs.pprint :refer [pprint]]
            [om.dom :as dom]))

(enable-console-print!)

(def initial-state {:last-error "" :new-person ""
                    ; Structure the UI state like the UI composes. Persistent (Ident) objects will become refs on normalization
                    :widget     {:people [
                                          {:db/id 1 :person/name "Linda" :person/mate {:db/id 2 :person/name "Tommy"}}
                                          {:db/id 2 :person/name "Tommy" :person/mate {:db/id 1 :person/name "Linda"}}
                                          ]}
                    ; storage for UI concerns for object with idents like [:db/id n]
                    ; UI tables will be named by adding ui. to the namespace of the ref keyword in the ident.
                    ; See om-tutorial.client-mutation/toggle-ui-boolean for a mutation
                    ; See om-tutorial.local-read/read-local for an example of how to read
                    :ui.db/id   {
                                 1 {:ui.db/id 1 :ui/checked false}
                                 2 {:ui.db/id 2 :ui/checked true}
                                 }
                    })

(def normalized-state (om/tree->db ui/Root initial-state true))

; The new-read-entry-point gives you SAX-style parsing, and separates the local parser from the remote fetch one(s)
; No more complected logic!
(def parser (om/parser {:read   (p/new-read-entry-point local/read-local {:my-server remote/read-remote})
                        :mutate m/mutate}))

(defn resolve-tempids [state tid->rid]
  (clojure.walk/prewalk #(if (-> % type (= om.tempid/TempId))
                          (get tid->rid %) %)
                        state))

(defn tempid-migrate [pure query tempids id-key] (resolve-tempids pure tempids))

(defonce reconciler (let [rv (om/reconciler {:state   initial-state
                                          :parser  parser
                                          :id-key  :db/id   ; REQUIRED for tempid migration to work
                                          :remotes [:my-server]
                                          :migrate tempid-migrate
                                          :pathopt true
                                          :send    remote/send})]
                      (om/add-root! rv ui/Root (gdom/getElement "app"))
                      rv
                      ))


