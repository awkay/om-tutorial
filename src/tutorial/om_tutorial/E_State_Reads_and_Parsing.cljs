(ns om-tutorial.E-State-Reads-and-Parsing
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc deftest]]
            [cljs.reader :as r]
            [om.next.impl.parser :as p]))

(let [sam {:db/id 1 :person/name "Sam" :person/mate [:people/by-id 2]}
      jenny {:db/id 2 :person/name "Jenny" :person/mate [:people/by-id 1]}
      app-state {:widget/people [[:people/by-id 1] [:people/by-id 2]]
                 :people/by-id  {1 sam 2 jenny}
                 }]
  (defcard-doc
    "
    # State Reads and Parsing

    As we said in the Queries section: the queries are composed to the root of the UI, and
    (except for optimization cases) looks as if it passed throught the root to whatever child
    needs it. Om actually does a little better than that, but you should think of it that way for
    now.

    So, how do you get Om to take the data out of the app state database (which you invented)
    and give it in the UI?

    The answer is that Om supplies you with a query grammar parser that understands the syntax
    of queries. As it hits each node of a query it is programmed (by you) to call read functions
    that retrieve the data.

    Fortunately, if you're using the default database format then you can get a lot of the work
    done for you by leveraging `db->tree`.

    ## Using `db->tree`

    You can do a lot of work to build parsing and reads, but Om comes with a very nice function for turning a UI query
    into the desired data from a normalized (default-format) database. With liberal use of idents as links in the graph
    almost all of your real state can be at the \"top\"  of the actual application state database.

    For example, given app state (the following are live tests you can play with in the source of this file):
    "
    app-state
    )
  (deftest db-tree-tests
           "
           - You can read a top-level ident query (no id part, special use of `_`):
           ```
           (om/db->tree '[[:widget/people _]] app-state app-state))) => {:widget/people [{:db/id 1, :person/name \"Sam\", :person/mate [:people/by-id 2]} {:db/id 2, :person/name \"Jenny\", :person/mate [:people/by-id 1]}]}
           ```
           "
           (is (= {:widget/people [sam jenny]} (om/db->tree '[[:widget/people _]] app-state app-state)))
           "- You can read a simple ident query with full ID:

           ```
           (om/db->tree '[[:people/by-id 2]] app-state app-state) => {[:people/by-id 2] {:db/id 2, :person/name \"Jenny\", :person/mate [:people/by-id 1]}}
           ```
           "
           (is (= {[:people/by-id 2] jenny} (om/db->tree '[[:people/by-id 2]] app-state app-state)))
           "- You can use an ident as the key to a join, and follow the graph to generate more of a tree:

           ```
           (om/db->tree '[{[:people/by-id 1] [:person/name {:person/mate [:person/name]}]}] app-state app-state) => {[:people/by-id 1] {:person/name \"Sam\", :person/mate {:person/name \"Jenny\"}}}
           ```
           "
           (is (= {[:people/by-id 1] {:person/name "Sam", :person/mate {:person/name "Jenny"}}}
                  (om/db->tree '[{[:people/by-id 1] [:person/name {:person/mate [:person/name]}]}] app-state app-state)))
           "- And you can even have a query-prefix of UI-related keys that are not even in the app-state database!


           ```
           (om/db->tree '[{:root-ui [{:widget [{[:people/by-id 1] [:db/id :person/name]}]}]}] app-state app-state) => {:root-ui {:widget {[:people/by-id 1] {:db/id 1, :person/name \"Sam\"}}}}
           ```
           "

           (is (= {:root-ui {:widget {[:people/by-id 1] {:db/id 1, :person/name "Sam"}}}}
                  (om/db->tree '[{:root-ui [{:widget [{[:people/by-id 1] [:db/id :person/name]}]}]}] app-state app-state)))
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

  DEPRECATED. Use `db->tree`.  The following code may aid in you understanding how `db->tree` works. The UI-related
  separation is more easily done by overriding the merge behaviors in Om.

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


  ")


