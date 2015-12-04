(ns om-tutorial.E-State-Reads-and-Parsing
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.util.edn-renderer :refer [html-edn]]
            [devcards.core :as dc :refer-macros [defcard defcard-doc deftest]]
            [cljs.reader :as r]
            [om.next.impl.parser :as p]))

(defcard-doc
  "
  # State Reads and Parsing

  ## The Problem

  Once you have your application state in a client database you need to get that data onto the screen.

  There are two interesting pieces to that puzzle:

  - Getting an initial UI on the screen
  - Asking for data from one or more servers, and having that novelty appear on the screen
  once it is returned.

  This is complicated by the fact that Om lets you define your database format. For the
  purposes of this tutorial, we're going to assume you're using the default database
  format, which has the distinct advantage of solving a lot of the problems for you.

  ## The Solution

  In this section we're only going to address the local state. Getting data from a server
  is in a later chapter.

  In the Queries section we stated the fact that the queries are composed to the root of the UI, and
  (except for optimization cases explained later) is passed through the root to whatever child
  needs it. Om actually does a little better than that, but you should think of it that way for
  now.

  So, how do you we Om to take the data out of the app state database (which you invented)
  and give it to the UI?

  The answer is that Om supplies you with a query grammar *parser* that understands the syntax
  of queries. You are required to create an instance of this parser, and plug in a state read
  function that does the conversion from the data item being requested to a result.

  Fortunately, if you're using the default database format then you can get a lot of the work
  done for you by leveraging `db->tree`, which knows how to convert non-parameterized queries
  into a proper state tree format needed for the return value of these reads.

  Before we just hand you that answer, you should understand the basics of the parser mechanism,
  since you will end up needing to do more than the provided `db->tree` can do alone.

  ## The Om Parser

  The Om parser is exactly what it sounds like: a parser for the query grammar. Now, formally
  a parser is something that takes apart input data and figures out what the parts mean (e.g.
  that's a join, that's a mutation call, etc.). In an interpreter, each time the parser finds
  a bit of meaning, it invokes a function to interpret that meaning and emit a result.
  In this case, the meaning is a bit of result data; thus, for Om to be able to generate a
  result from the parser, you must supply the \"read\" emitter.

  First, let's see what an Om parser in action.
  ")

(defcard om-parser
         "This card will run an Om parser on an arbitrary query, record the calls to the read emitter,
         and show the trace of those calls in order. Feel free to look at the source of this card.

         Essentially, it creates an Om parser:

         ```
         (om/parser {:read read-tracking})
         ```

         where the `read-tracking` simply stores details of each call in an atom and shows those calls
         when parse is complete.

         The signature of a read function is:

         `(read [env dispatch-key params])`

         where the env contains the state of your application, a reference to your parser (so you can
         call it recursively, if you wish), a query root marker, an AST node describing the exact
         details of the element's meaning, a path, and *anything else* you want to put in there if
         you call the parser recursively.

         Try some queries like these:

         - `[:a :b]`
         - `[:a {:b [:c]}]` (note that the AST is recursively built, but only the top keys are actually parsed to trigger reads)
         - `[(:a { :x 1 })]`  (note the value of params)
         "
         (fn [state _]
           (let [{:keys [v error]} @state
                 trace (atom [])
                 read-tracking (fn [env k params]
                                 (swap! trace conj {:env          (assoc env :parser :function-elided)
                                                    :dispatch-key k
                                                    :params       params}))
                 parser (om/parser {:read read-tracking})]
             (dom/div nil
                      (when error
                        (dom/div nil (str error)))
                      (dom/input #js {:type     "text"
                                      :value    v
                                      :onChange (fn [evt] (swap! state assoc :v (.. evt -target -value)))})
                      (dom/button #js {:onClick #(try
                                                  (reset! trace [])
                                                  (swap! state assoc :error nil)
                                                  (parser {:state {:app-state :your-app-state-here}} (r/read-string v))
                                                  (swap! state assoc :result @trace)
                                                  (catch js/Error e (swap! state assoc :error e))
                                                  )} "Run Parser")
                      (dom/h4 nil "Parsing Trace")
                      (html-edn (:result @state))
                      )))
         {}
         {:inspect-data false})

(defn read-person [env dispatch-key params]
  (case dispatch-key
    :name {:value "Sally"} ; important...wrap real result values in a map with key :value
    :age {:value 23}
    :not-found
    ))
(def person-parser (om/parser {:read read-person}))
(def person-query [:name :age])

(defcard-doc
  "To indicate that you've found data your read function simply returns the value for the item being requested
  wrapped in a map with the key `:value`. So, if you were writing
   a really simple static application, your read function can just invent the data...no need for a database
   at all. Your read function could just be:
   "
  (dc/mkdn-pprint-source read-person)
  "and the parser:"
  (dc/mkdn-pprint-source person-parser)
  "which when run on this query:"
  (dc/mkdn-pprint-source person-query)
  "using:"
  '(person-parser {:state :none} person-query)
  "will result in this value:"
  (person-parser {:state :none} person-query)
  "Notice that the output of the parser is a properly structured map matching the query structure.

  Another way of looking at what the parser does for state reads is that it constructs the *structure* of
  the result, and calls your read function to supply the data at each node of that structure.

  ## More Advanced Parsing

  So far we've seen the following things about parsing:

  - Parsing a query calls a read function for each top-level item in a query
  - The read function may return a value within a map, under the key `:value`
  - The parser will generate the result map (for these top-level keys) and fill in the values
  that the read function returned.

  In order to build a full result tree, you're going to have to follow the joins. At some
  point you'll reach a point where you'll need to manually do this. We'll cover that in a later
  section. For now, realize that if nothing else was done for you, then your read functions
  would need to recursively call the parser. Fortunately, if you're using the default database
  format then there is already a helper function that can do that part for most data-only
  queries: `db->tree`.

  NOTE: This is where I'm working on the tutorial...

  Plan
  - Explain a more detailed read function that can suffice for much of the state reading, and is still
  extensible:
  ```
  (defn read-local ; assumes it is only ever called on top-level query, or starting at an ident
    [{:keys [query state ast] :as env} dkey params]
    (case dkey
      :app/locale (deref i18n/*current-locale*) ; non-database item(s)...global atom, e.g.
      (let [top-level-prop (nil? query) ; joins will have a query
            key (or (:ast key) dkey) ; use key from AST, unless it is not there
            by-ident? (om/ident? key) ; detect if the query is based at an ident
            data (if by-ident? (get-in @state key) (get @state key))] ; get the 'base state' for conversion
        {:value (if top-level-prop
                  data
                  (om/db->tree query data @state))})))
  ```

  "
  )

(let [sam {:db/id 1 :person/name "Sam" :person/mate [:people/by-id 2]}
      jenny {:db/id 2 :person/name "Jenny" :person/mate [:people/by-id 1]}
      app-state {:widget/people [[:people/by-id 1] [:people/by-id 2]]
                 :people/by-id  {1 sam 2 jenny}
                 }]
  (defcard-doc "
    ## Using `db->tree`

    Om comes with a very nice function for turning a UI query
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


