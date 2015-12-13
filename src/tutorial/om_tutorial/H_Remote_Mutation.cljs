(ns om-tutorial.H-Remote-Mutation
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "
  # Remote Mutation

  Remote mutation is done very much like remote fetch. Any time you run `transact!` Om will invoke
  your parser on the expression. Any calls within that expression will trigger calls to mutate. In
  remote mode (target != nil), you do the same thing you do for remote fetch: return a
  map that says (here I'm assuming you have only one remote and you've left it named
  `:remote`) `{:remote true}` or `{:remote ast}`. The latter allows you to modify the
  expression actually sent to the server.

  So, for example in your UI you might trigger:

  ```
  (transact! this '[(app/save-thing)])
  ```

  and your mutate function has nothing to do locally, but it wants to include the
  actual data to save in the remote call:

  ```
  (defn mutate-save-thing [{:keys [ast]} key params]
     { :remote (assoc ast :params { :data { ... } }) })
  ```

  Now your send function will be called with this modified save, which includes
  the actual data to pass along to the server.

  TODO: Continue working on this section...

  ## Temporary IDs

  Generating new items on a client is a very common task. When you do this you want to be able to
  later reassign these temporary ids to whatever the server decided they should be.

  Steps:

  + When you create something new, use (om/tempid) to generate an id for it. Store this in that
  new object's map at a consistent key (defined by passing :id-key as a config parameter to the
  reconciler).
  + When you send the new thing over the network, make sure you're using om/transit, since that
  can serialize TempId data types.
  + On the server, convert tempids to real IDs. Return a :tempids map from the server. Keys are tempid ident, the
   values are the new real ID idents.
  + Built-in migrate of Om should rewrite the tempids to the real ones automatically on callback, IF:
    + The items returns are present in the UI query from root
    + The :id-key for the id on the object maps is set in reconciler.

  TODO: Finish this...notes:

  - Temporary ID handling is handled for you with the default database format for any UI that is currently in the
  root components query and also has an Ident.
  - Since your UI components may use anything for an ident keyword, you must specify the real object ID keyword via
  the reconciler's config parameter `:id-key`.
  - See `om-tutorial.om-specs` for working example
  - Action method on server-side must be the thing that figures out the new ID (side-effect free). The return value
  of the action methods ends up as :result on parser return...you must post-process that or you'll send the wrong
  stuff to the client.
  - See `simulated-server` for hints on how to fix up the results of mutation parses
  - You MUST set `:id-key` on reconciler to the ID key used by the actual objects to be migrated, since the ident need
  not use that key (idents are a client-local concern)

  ")
