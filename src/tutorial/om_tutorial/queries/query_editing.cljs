(ns om-tutorial.queries.query-editing
  (:require [om.next :as om :refer-macros [defui]]
            [cljs.reader :as r]
            [devcards.util.edn-renderer :refer [html-edn]]
            [goog.dom :as gdom]
            [goog.object :as gobj]
            [cljs.pprint :as pp :refer [pprint]]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addons.matchbrackets]
            [cljsjs.codemirror.addons.closebrackets]
            [om.dom :as dom]))

(defn run-query [db q]
  (try
    (om/db->tree (r/read-string q) db db)
    (catch js/Error e "Invalid Query")))

(def cm-opts
  #js {:fontSize          8
       :lineNumbers       true
       :matchBrackets     true
       :autoCloseBrackets true
       :indentWithTabs    false
       :mode              #js {:name "clojure"}})

(defn pprint-src
  "Pretty print src for CodeMirro editor.
  Could be included in textarea->cm"
  [s]
  (-> s
      r/read-string
      pprint
      with-out-str))


(defn textarea->cm
  "Decorate a textarea with a CodeMirror editor given an id and code as string."
  [id code]
  (let [ta (gdom/getElement id)]
    (js/CodeMirror
      #(.replaceChild (.-parentNode ta) % ta)
      (doto cm-opts
        (gobj/set "value" code)))))

(defui QueryEditor
       Object
       (componentDidMount [this]
                          (let [{:keys [query id]} @(om/props this)
                                src (pprint-src query)
                                cm (textarea->cm id src)]
                            (om/update-state! this assoc :cm cm)))
       (render [this]
               (let [props (om/props this)
                     local (om/get-state this)]
                 (dom/div nil
                          (dom/h4 nil "Database")
                          (html-edn (:db @props))
                          (dom/hr nil)
                          (dom/h4 nil "Query Editor")
                          (dom/textarea #js {:id (:id @props)})
                          (dom/button #js {:onClick #(let [query (.getValue (:cm local))]
                                                      (swap! props assoc :query-result (run-query (:db @props) query)
                                                             :query query))} "Run Query")
                          (dom/hr nil)
                          (dom/h4 nil "Query Result")
                          (html-edn (:query-result @props))))))

(def query-editor (om/factory QueryEditor))

