(defproject om-tutorial "0.1.0-SNAPSHOT"
  :description "My first Om program!"
  :source-paths ["src" "cards"]
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [devcards "0.2.1" :exclusions [org.omcljs/om]]
                 [org.omcljs/om "1.0.0-alpha22"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "test"]])
