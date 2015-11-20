(ns om-tutorial.B-UI
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defui Widget
       Object
       (render [this]
               (dom/div nil "Hello world")))

(defui WidgetWithHook
       Object
       (componentWillUpdate [this nextprops nextstate] (println "Component will update"))
       (render [this]
               (dom/div nil "Hello world")))

(def widget (om/factory Widget))


(defcard-doc
  "
  # UI
  
  Om uses React underneath. The primary mechanim for creating components is the `defui` macro:"
  (dc/mkdn-pprint-source Widget)
  "This macro generates a React Class as a plain javascript class, so it is completely compatible with the
  React ecosystem."
  "## React hooks
  
  If you wish to provide hook methods, you can define them under the Object section of the UI:"
  (dc/mkdn-pprint-source WidgetWithHook)
  "
  ## Element factory
  
  You generate a factory for elements of this class with `om/factory`. This generates function that
  acts like a new 'tag' for your DOM:"
  (dc/mkdn-pprint-source widget)
  "and you can render these as plain React components in a devcard, which makes fine tuning them as pure UI dead simple:
  
  ```
  (defcard widget-card (widget {}))
  ```
  "
  (dc/mkdn-pprint-source widget-card)
  "The resulting "
  )

(defcard widget-card
         (widget {}))

