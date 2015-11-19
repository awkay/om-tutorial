(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om-tutorial.parsing :as p]
            [om-tutorial.ui :as ui]
            [om-tutorial.client-mutation :as m]
            [om-tutorial.local-read :as local]
            [om-tutorial.remote-fetch :as remote]
            [om-tutorial.simulated-server :as server]
            [om.dom :as dom]))

(enable-console-print!)

(def initial-state {:last-error "" :new-person "" :people :missing})

(def parser (om/parser {:read (p/new-read-entry-point local/read-local {:my-server remote/read-remote}) 
                        :mutate m/mutate}))

(def reconciler (om/reconciler {:state   initial-state
                                :parser  parser
                                :remotes [:my-server]
                                :send    server/send}))

(om/add-root! reconciler ui/Root (gdom/getElement "app"))


