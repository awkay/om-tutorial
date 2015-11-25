(ns om-tutorial.E-State-Reads-and-Parsing
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "
  # State Reads and Parsing

  Now that we understand the format of the default database, the basics of the query grammar, and how we're going to
  locate those queries on the UI it is time to understand how we get data from the app database onto that UI. The
  join point is code that you write and plug into the Om parser to help it transfer data from your client app
  state database to the UI.


  ##

  TODO. Placeholder stuff from readme below...

  ## Parsing

  ## Joins

  ### Local vs. Remote

  The Om parser accepts just one read and one mutate. Unfortunately, this means that the same code gets invoked
  for both the local query processing (to data for rendering) and again for asking \"what do you want from remote (s)\".

  I've written a factory function that generates a dispatcher that separates this logic called
  `new-read-entry-point` (see `om-tutorial.core` for an example use). The generated function also has the ability
  to switch to dispatching to an alternate function during processing simply by associating that new function
  with the `:reader` key in `env`.

  This allows you to control which reader function is used based on the structure of the query instead of the keywords;
  however, you'll also find that the addition parser helpers reduce the amount of code you need to write by quite
  a lot.

  The setup is pretty easy. See `om-tutorial.core`:

  ```
  (def parser (om/parser {:read   (p/new-read-entry-point local/read-local {:my-server remote/read-remote})
                          :mutate m/mutate}))
  ```

  ### Local Parsing

  The parsing helper code in this example has been written with some care, with the intent to reduce the overall footprint
  of the application-specific local read code to a minimal level. The resulting helper functions are not tested
  across a large amount of database structure, but they seem to work well so far and will be improved as
  I work on it. I've attempted to give you a way to read \"the thing that should be here in the UI state\", and
  a way to \" follow that ref \". This reduces the total number of lines of client-specific read code for this example to
  just 5 lines!

  ```
  (defn read-local [env key params]
    (case key
      :ui/checked {:value (p/ui-attribute env key) } ; get a non-persistent UI bit of data
      :person/mate {:value (p/parse-join-with-reader read-local env key :limit 2)} ; to-one join, with recursion limit
      :people {:value (p/parse-join-with-reader read-local env key)} ; to-many join
      :widget {:value (p/parse-join-with-reader read-local env key :reset-depth 0)} ; to-one join, with recursion counter reset
      (p/db-value env key) ; just get the value that is at the \" current location \" in the database
      ))
  ```

  Basically, you must use a \"default\" database format of Om, which basically means a normalized one where
  anything with an Ident has been stuffed into root-level tables. The parsing helpers assume that the
  rest of your app state will follow the UI tree structure.

  The parsing is fine if you want to use the special keyword `:missing` in place of data that will be demand loaded
  later. The join processing code will naturally stop at such places when looking for local data.

  ### Separating UI-concern data from Persistent data

  If you have a widget that has only client-local data, then just put it on that widget.

  However, one trouble you'll likely run into pretty quickly is the fact that queries often complect local UI-concern data
  with stuff that is stored on a server. If you try to stuff these transient values into the same locations and
  do a remote fetch you'll either overwrite them, or you'll end up writing merge logic and plugging that in. Basically,
  you'll make a mess.

  Rather that fooling with that I've come up with a scheme where you can put any UI-specific data on a separate
  app-state table as long as the component in question has an `Ident`. Since most things that are persistent can
  (and probably will) have that, it seems like a non-intrusive requirement.

  The parsing read helpers for local state then make it *look like* these UI attributes are on the persistent object,
  when in fact they're pulled from a separate UI attribute table and merged in during the read.

  ### Basic read-local Rules

  Write the `read-local` using a `case`. These are very fast. You may dispatch to a different read function as
  you descend your state (by passing that function as the first argument to `parse-join-with-reader`, but you do not have to.

  The *default* case should just be `(p/db-value env key)`. The trick is that the join parser will add `:db-path` to
  the env, keeping track of where you are in the application state. The `db-value` function walks that path (following any
  Om refs (e.g. `[:db/id 4]` it hits), and returns the value of `key` at that place in the state. Thus, if none of the
  other keys make any noise about wanting to handle the read, it will try to get it from the current parse path.

  Any UI cases that you've designed in should work by handing them off to `ui-attribute`.

  Finally, any joins (singular or many) can be handled by the `parse-join-with-reader`. This descends the app state
  at that key (following refs), and recursively processes the query. Since refs can create infinite loops,
  it defaults to stopping recursion at a depth of 20. You can change this by using the `:limit n` parameter. Also,
  the depth is tracked from the first join. If you want to track from the current join, use `:reset-depth 0`.

  When objects are found, this combination will automatically filter out unwanted attributes. For example, if you
  ask for `[:a]` in some sub-fragment, and the object in the state there has `{:a 1 :b 2}`, then this parser code
  will return `{:a 1}`.

  ## Path Optimization

  If your UI gets rather large, you may see warnings in the Javascript Console of the browser about slowness. If you do,
  you can leverage path optimization to minimize the amount of work the parser has to do in order to update a sub-portion
  of the UI.

  If you pass `:pathopt true` to the reconciler, then Om will attempt to root a query at the component that needs
  re-rendering if and only if it has an `Ident`.

  When it attempts this, it will call your `read` function with `:query/root` set to the ident of the component that
  is needing re-render, and

  ")
