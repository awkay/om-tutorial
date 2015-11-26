(defproject om-tutorial "0.1.0-SNAPSHOT"
  :description "A Tutorial for Om 1.0.0 (next)"
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [devcards "0.2.1" :exclusions [org.omcljs/om]]
                 [org.omcljs/om "1.0.0-alpha24"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "test"]]

  :source-paths ["src/main" "src/cards" "src/tutorial"]

  :clean-targets ^{:protect false} ["resources/public/js" "resources/public/cards" "resources/public/tutorial" "target"]

  :figwheel {:build-ids   ["dev" "cards" "tutorial"]
             :server-port 3450}

  :cljsbuild {
              :builds
              [
               {:id           "dev"
                :figwheel     true
                :source-paths ["src/main"]
                :compiler     {:main                 om-tutorial.core
                               :asset-path           "js"
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "resources/public/js"
                               :recompile-dependents true
                               :parallel-build       true
                               :verbose              false}}
               {:id           "cards"
                :figwheel     {:devcards true}
                :source-paths ["src/main" "src/cards"]
                :compiler     {
                               :main                 om-tutorial.cards
                               :source-map-timestamp true
                               :asset-path           "cards"
                               :output-to            "resources/public/cards/cards.js"
                               :output-dir           "resources/public/cards"
                               :recompile-dependents true
                               :parallel-build       true
                               :verbose              false}}
               {:id           "tutorial"
                :figwheel     {:devcards true}
                :source-paths ["src/main" "src/tutorial"]
                :compiler     {
                               :main                 om-tutorial.tutorial
                               :source-map-timestamp true
                               :asset-path           "tutorial"
                               :output-to            "resources/public/tutorial/tutorial.js"
                               :output-dir           "resources/public/tutorial"
                               :parallel-build       true
                               :recompile-dependents true
                               :verbose              false}}]}

  :profiles {
             :dev {:source-paths ["src/dev"]
                   :repl-options {:init-ns user
                                  :port    7001}
                   }
             }
  )
