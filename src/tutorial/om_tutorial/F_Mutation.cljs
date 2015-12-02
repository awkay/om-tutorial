(ns om-tutorial.F-Mutation
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "
  # Mutation

  Mutations are part of the query syntax, except they dispath to your top-level mutation function. One way
  to deal with this nicely is to use a multi-method, dispatched by symbol.

  Any actions that you want to run locally just go in the action thunk of the return of your mutation.

  The UI-tree portion of your app state will end up with a bunch of refs in it, and the top-level of the state
  will have Om-generated tables holding the objects themselves.

  Here are some notes on the various kinds of operations you'll want to do:

  - Create
      - Use `om/tempid` to get a temp id for the new object
      - Add the object to the top level table (e.g. `(swap! state update-in [:db/id ] assoc tmpid obj)`
      - Add refs (e.g. `[:db/id tmpid]`) to any UI components that should be showing that new object
      - Optionally return a remote ast to indicate you want to send the request to the server
  - Read (covered in the read section)
  - Update
      - Simply update the state in the \"tables\".
  - Delete
      - Delete the refs from the UI state portion of state. You can leave the object in the tables (in case, say,
        other UI components are looking at it, or you might want to undo the delete)

  After doing a mutation, you can trigger re-renders by listing query bits after the mutation. Any keywords you list
  will trigger re-renders of things that queried for those keywords. Any refs (e.g. `[:db/id 4]`) will trigger
  re-renders of anything that has that Ident. In the example below, anything that has included the prop named
  `:widget` or has the Ident `[:db/id 4]` will re-render after the operation.

  ```
     (om/transact! this '[(app/do-thing) :widget [:db/id 4]])
  ```
  ")

