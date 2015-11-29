(ns user
  (:require
    [clojure.pprint :refer (pprint)]
    [clojure.stacktrace :refer (print-stack-trace)]
    [figwheel-sidecar.repl-api :as ra]
    ))

(def figwheel-config
  {:figwheel-options {:server-port 3450}
   ;; builds to focus on
   ;:build-ids        ["tutorial" "cards" "dev"]
   :build-ids        ["dev" ]
   ;; load build configs from project file
   :all-builds       (figwheel-sidecar.config/get-project-builds)
   })


(comment
  (-> figwheel-config :all-builds first)
  "Coming soon..."
  (defonce nrepl-server (atom nil))

  (defn kill-nrepl
    "Kill the currently booted nREPL"
    [] (swap! nrepl-server nrepl/stop-server))

  (defn boot-nrepl
    "Start an nREPL on the given port."
    [port]
    (when @nrepl-server (kill-nrepl))
    (reset! nrepl-server (nrepl/start-server :port port))))

(defn start-dev
  "Start Figwheel and fw repl. You should be running this namespace from PLAIN clojure.main NOT nREPL!

  nREPL support can be had (for server-side code) in parallel, but I've not finished adding it yet (since
  there is no server code yet).
  "
  []
  ;(boot-nrepl 7080)
  (ra/start-figwheel! figwheel-config)
  (ra/cljs-repl))

