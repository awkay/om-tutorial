(require '[figwheel-sidecar.repl :as r]
         '[figwheel-sidecar.repl-api :as ra])

(ra/start-figwheel!
  {:figwheel-options { :server-port 3450}
   :build-ids ["dev" "cards"]
   :all-builds
   [
    {:id "dev"
     :figwheel true
     :source-paths ["src"]
     :compiler {:main 'om-tutorial.core
                :asset-path "js"
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js"
                :verbose true}}
    {:id "cards"
     :figwheel {:devcards true}
     :source-paths ["src" "cards"]
     :compiler {
                :main 'om-tutorial.cards
                :source-map-timestamp true
                :asset-path "cards"
                :output-to "resources/public/cards/cards.js"
                :output-dir "resources/public/cards"
                :verbose true}}]})

(ra/cljs-repl)
