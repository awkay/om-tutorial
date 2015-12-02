(ns user
  (:require
    [figwheel-sidecar.repl-api :as ra]
    ))

(def figwheel-config
  {:figwheel-options {:server-port 3450}
   ;; builds to focus on
   :build-ids        [ "tutorial" ]
   ;; load build configs from project file
   :all-builds       (figwheel-sidecar.config/get-project-builds)
   })


(defn start-dev
  "Start Figwheel and fw repl. You should be running this namespace from PLAIN clojure.main NOT nREPL!

  nREPL support can be had (for server-side code) in parallel, but I've not finished adding it yet (since
  there is no server code yet).
  "
  []
  (ra/start-figwheel! figwheel-config)
  (ra/cljs-repl))

(start-dev)
