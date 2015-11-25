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

  TODO

  ## Temporary IDs

  Temporary ID handling is handled for you with the default database format for any UI that is currently in the
  root components query and also has an Ident.

  Since your UI components may use anything for an ident keyword, you must specify the real object ID keyword via
  the reconciler's config parameter `:id-key`.

  More coming...

  Notes:

  - See `om-tutorial.om-specs` for working example
  - Action method on server-side must be the thing that figures out the new ID (side-effect free). The return value
  of the action methods ends up as :result on parser return...you must post-process that or you'll send the wrong
  stuff to the client.
  - See `simulated-server` for hints on how to fix up the results of mutation parses
  - You MUST set `:id-key` on reconciler to the ID key used by the actual objects to be migrated, since the ident need
  not use that key (idents are a client-local concern)

  ")
