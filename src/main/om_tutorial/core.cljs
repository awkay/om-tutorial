(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om-tutorial.parsing :as p]
            [om-tutorial.ui :as ui]
            [om-tutorial.client-mutation :as m]
            [om-tutorial.local-read :as local]
            [om-tutorial.client-remoting :as remote]
            [om-tutorial.simulated-server :as server]
            [om.dom :as dom]))

(enable-console-print!)

(def initial-state {:last-error "" :new-person ""
                    ; Structure the UI state like the UI composes. Persistent (Ident) objects will become refs on normalization
                    :widget     {
                                 :people [{:db/id 1 :person/name "Tony" :garbage 1 :person/mate {:db/id 2 :a 5 :person/name "Jane"}}
                                          {:db/id 2 :person/name "Jane" :garbage 2 :person/mate {:db/id 1 :b 2 :person/name "Tony"}}]}
                    ; storage for UI concerns for object with idents like [:db/id n]
                    ; UI tables will be named by adding ui. to the namespace of the ref keyword in the ident.
                    ; See om-tutorial.client-mutation/toggle-ui-boolean for a mutation
                    ; See om-tutorial.local-read/read-local for an example of how to read
                    :ui.db/id   {
                                 1 {:ui.db/id 1 :ui/checked false}
                                 2 {:ui.db/id 2 :ui/checked true}
                                 }
                    })

; The new-read-entry-point gives you SAX-style parsing, and separates the local parser from the remote fetch one(s)
; No more complected logic!
(def parser (om/parser {:read   (p/new-read-entry-point local/read-local {:my-server remote/read-remote})
                        :mutate m/mutate}))

(def reconciler (om/reconciler {:state   initial-state
                                :parser  parser
                                :remotes [:my-server]
                                :send    remote/send}))

(om/add-root! reconciler ui/Root (gdom/getElement "app"))


(comment
  (def q
    (parser {:state (atom { :widget {:people :missing}})} '[{:widget [{:people [:ui/checked :db/id :person/name {:person/mate ...}]}]}] :my-server))

  (let [bit (-> q first :widget first)]
    (println bit)
    (println (meta bit))
    )

  )