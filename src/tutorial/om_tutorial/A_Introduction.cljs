(ns om-tutorial.A-Introduction
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc 
  "# Introduction
  
  This tutorial will walk you through the various parts of Om 1.0 (alpha). In order to get the most from this 
  tutorial, you should understand the general goals of Om (next):
  
  - Keep application state in a single top-level atom.
  - Provide a mechanism whereby clients can:
      - Make precise non-trivial reads simply
      - Allow clients to communicate non-trivial operations simply.
  - Eliminate the need for event models
  - Provide a synchronous programming model
  
  There are others, but this is sufficient for a starting point.
  
  ## General Components
  
  The following significant areas of Om must be understood in order to write a non-trivial application.
  
  - Building the UI.
  - Queries and the Query Grammar.
  - Colocating query fragments on stateful UI component (for composition and local reasoning).
  - The client-local app state database.
  - Turning the Queries into data for your UI.
  - Turning the Queries into remote requests for data.
  - Processing incoming responses to remote requests.
  - Dynamically changing Queries
  
  Let's start with the [UI](#!/om_tutorial.B_UI).
  
  ## Notes on documentation:
  
  I'm using devcards to render a lot of source from actual code; Unfortunately devcards cannot handle reader tags, 
  so I apologize about the use of `clj->js`... Note that you'd normally write `#js { :onClick ...}` instead.
  ")
