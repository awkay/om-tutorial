(ns om-tutorial.G-Remote-Fetch
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "
  # Remote Fetch
  
  TODO

  ### Remote Fetch 

  For each remote that you list in the reconciler (default is just `:remote`), the parser will run with `:target` set
  in the env to that remote. This lets you gather up different sets of queries to send to each remote.

  The reader factory I've created lets you supply a map from remote name to reader function, so that you can 
  separate your logic out for each of these query parses.

  In remote parsing mode, the parser expects you to return either `true` or a (possibly modified) AST node (which
  comes in as `:ast` in `env`). Doing recursive parsing on this is a bit of a pain, but is also typically necessary
  so that you can both maintain the structure of the query (which *must* be rooted from your Root component)
  and prune out the bits you don't want.

  The remote read in this example (so far) only wants a list of people. Everything else is client-local. Using the 
  parsing helpers in the `om-tutorial.parsing` namespace, this pares down to this:

  ```
  (defn read-remote [env key params]
    (case key
      :widget (p/recurse-remote env key true)
      :people (p/fetch-if-missing env key :make-root)
      :not-remote ; prune everything else from the parse
      )
    )
  ```

  The `recurse-remote` function basically means \"I have to include this node, because it is on the path to real
    remote data, but it itself needs nothing from the server\". The `fetch-if-missing` function has quite a bit 
  of logic in it, but basically means \"Everything from here down is valid to ask the server about\".

  The `:make-root` flag (which can be boolean or any other keyword, but only has an effect if it is `:make-root` or `true`)
  is used to set up root processing. I'll cover that more later.

  TODO: Elide keywords from the resulting fetch query if they are in the ui.* namespace, so we don't ask the server for them

  ### Server simulation

  The present example has a server simulation (using a 1 second setTimeout). Hitting \"refresh\" will clear the `:people`, 
  which will cause the remote logic to trigger. One second later you should see the simulated data I've placed on this
  \"in-browser server\".

  There is a lot more to do here, but tempids are not quite done yet, so I'll add more in as that becomes available.
  ")
