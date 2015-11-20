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

(defui WidgetWithProperties
       Object
       (render [this]
               (let [{:keys [name]} (om/props this)]
                 (dom/div nil (str "Hello " name)))))

(def prop-widget (om/factory WidgetWithProperties))


(defcard-doc
  "
  # UI
  
  Om uses <a href=\"https://facebook.github.io/react/index.html\" target=\"_blank\">React</a> underneath.
  The primary mechanim for creating components is the `defui` macro:"
  (dc/mkdn-pprint-source Widget)
  "This macro generates a React Class as a plain javascript class, so it is completely compatible with the
  React ecosystem."
  "## React Lifecycle Methods
  
  If you wish to provide <a href=\"https://facebook.github.io/react/docs/component-specs.html#lifecycle-methods\"
  target=\"_blank\">lifecycle methods</a>, you can define them under the Object section of the UI:"
  (dc/mkdn-pprint-source WidgetWithHook)
  "
  ## Element factory
  
  You generate a factory for elements of this class with `om/factory`. This generates a function that
  acts like a new 'tag' for your DOM:"
  (dc/mkdn-pprint-source widget)
  "and you can render these as plain React components in a <a href=\"https://github.com/bhauman/devcards#devcards\"
  target=\"_blank\">devcard</a>, which makes fine tuning them as pure UI dead simple:
  
  ```
  (defcard widget-card (widget {}))
  ```
  "
  "The resulting card looks like this:"
  )

(defcard widget-card
         (widget {})
         )

(defcard-doc
  "You can send properties to such a stateless thing as a simple edn map, and pull them out of `this` using 
  `om/props`."
  (dc/mkdn-pprint-source WidgetWithProperties)
  (dc/mkdn-pprint-source prop-widget)
  "
  ```
  (defcard props-card (prop-widget {:name \"Sam\"}))
  ```
  "
  )

(defcard props-card (prop-widget {:name "Sam"}))

(defui Person
       Object
       (render [this]
               (let [{:keys [name]} (om/props this)]
                 (dom/li nil name))))

(def person (om/factory Person {:keyfn :name}))

(defui PeopleList
       Object
       (render [this]
               (let [people (om/props this)]
                 (dom/ul nil (map person people))
                 )))

(def people-list (om/factory PeopleList))

(defui Root
       Object
       (render [this]
               (let [{:keys [people number]} (om/props this)]
                 (dom/div nil
                          (dom/span nil (str "My lucky number is " number " and I have the following friends:"))
                          (people-list people))
                 )))

(def root (om/factory Root))

(defcard-doc
  "
  ## Composing the UI
  
  Composing these is pretty straightforward: pull out the bits from props, and pass them on to subcomponents. 
  "
  (dc/mkdn-pprint-source Person)
  (dc/mkdn-pprint-source PeopleList)
  (dc/mkdn-pprint-source Root)
  (dc/mkdn-pprint-source people-list)
  (dc/mkdn-pprint-source person)
  (dc/mkdn-pprint-source root)
  "
  
  ```
  (defcard root-render (root {:number 52 :people [{:name \"Sam\"} {:name \"Joe\"}]}))
  ```
  
  It is important to note that _this is exactly how the composition of UI Components always happens_, independent of
  whether or not you use the rest of the features of Om. A root component calls the factory functions of subcomponents
  with an edn map as the first argument. That map is accessed using `om/props` on `this` within the subcomponent. Data
  is passed from component to component through `props`.
  "
  )

(defcard root-render (root {:number 52 :people [{:name "Sam"} {:name "Joe"}]}))

(defui Root-computed
       Object
       (render [this]
               (let [{:keys [people number b]} (om/props this)
                     {:keys [incHandler boolHandler]} (om/get-computed this)]
                 (dom/div nil
                          ; devcards cannot deal with #js on rendering source. Using clj->js instead
                          (dom/button (clj->js {:onClick #(boolHandler)}) "Toggle Luck")
                          (dom/button (clj->js {:onClick #(incHandler)}) "Increment Number")
                          (dom/span nil (str "My " (if b "" "un") "lucky number is " number
                                             " and I have the following friends:"))
                          (people-list people))
                 )))

(def root-computed (om/factory Root-computed))

(defcard-doc
  "
  ## Out-of-band Data
  
  In plain React, you pass stuff through props. In Om, props is meant to take a slightly different role: The properties
  of the component that have to do with state. Particularly queried state, which we'll cover later.

  Because of internal requirements having to do with efficient re-rendering (among others), you should not pass
  \"computed\" things (e.g. callbacks) through props.
  
  Instead Om has helper functions for hanging this computed information in a side-band channel of props. So, in the
  example below you'll see that a callback is being passed via `om/computed` and `om/get-computed`. The 
  former attaches the extra bits to the props, and the latter pulls them out.
  
  Sorry about the use of `clj->js`...devcards can't currently render the source of something with reader tags in it. Note
  that you'd normally write `#js { :onClick ...}`.
  "

  (dc/mkdn-pprint-source Root-computed)
  (dc/mkdn-pprint-source root-computed)

  "The resulting card (with modifiable state) looks like this:
  ```
  (defcard passing-callbacks-via-computed
         (fn [data-atom-from-devcards _]
           (let [prop-data @data-atom-from-devcards
                 sideband-data {:incHandler  (fn [] (swap! data-atom-from-devcards update-in [:number] inc))
                                :boolHandler (fn [] (swap! data-atom-from-devcards update-in [:b] not))}
                 ]
             (root-computed (om/computed prop-data sideband-data)))
           )
         {:number 42 :people [{:name \"Sally\"}] :b false}
         {:inspect-data true})
  ```
  "
  )

(defcard passing-callbacks-via-computed
         (fn [data-atom-from-devcards _]
           (let [prop-data @data-atom-from-devcards
                 sideband-data {:incHandler  (fn [] (swap! data-atom-from-devcards update-in [:number] inc))
                                :boolHandler (fn [] (swap! data-atom-from-devcards update-in [:b] not))}
                 ]
             (root-computed (om/computed prop-data sideband-data)))
           )
         {:number 42 :people [{:name "Sally"}] :b false}
         {:inspect-data true})
