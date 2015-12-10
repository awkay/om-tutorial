(ns om-tutorial.E-State-Reads-and-Parsing
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [om-tutorial.state-reads.parser-1 :as parser1]
            [om-tutorial.state-reads.parser-2 :as parser2]
            [om-tutorial.state-reads.parser-3 :as parser3]
            [devcards.util.edn-renderer :refer [html-edn]]
            [devcards.core :as dc :refer-macros [defcard defcard-doc deftest]]
            [cljs.reader :as r]
            [om.next.impl.parser :as p]))

(defcard-doc
  "
  # State Reads and Parsing

  First, make sure you've read the App Database section carefully, as we'll be leveraging that understanding here.

  Next, please understand that *Om does not know how to read your data* on the client *or* the server.
  It does provide some useful utilities (e.g. `db->tree`) for the default database format,
  but since you can do more advanced things like parameterize joins, use alternate databases, and
  structure things in the database in some arbitrary way you almost certainly will
  have to participate in query result construction.

  Another thing to understand is that all application state changes happen through the query grammar
  via abstract operations, and the interpretation of those operations is done via the same parsing
  subsystem. In this case you have to define everything that happens, since it is your application
  state you are changing.

  Finally, when talking with servers you use the exact same mechanisms (query processing) to
  figure out what part of the UI state should come from your servers, and what operations
  of mutation have a remote component.
  In this case it is completely up to you to understand what you're doing with respect to
  query processing. We'll talk about mutations and remote fetch in a later chapter.

  Simply put: you really can't write much of an Om application without getting involved
  with interpreting the query/mutation grammar. In fact, much of your application's
  complexity will move to this level.

  In the section on Queries you learned that the function `db->tree` can convert a
  default-format database into a desired UI tree. All of the examples in that section
  used that utility to run your arbitrary queries. You will want to leverage that
  wherever you can; however, there are many cases where this utility function is insufficient.
  Examples include:

  1. You want to use parameters on query elements (on a property, join, etc). Examples
  might include pagination, sorting, filtering, etc.
  2. Your UI query somehow doesn't match up with the data. Examples might include cases where
  you want to group a set of elements by some attribute on those elements.
  3. In remote interactions, you need to walk the query in order to determine which
  parts should interact with one or more servers.

  You can write really simple apps with `db->tree`, and when you reach a point in the query
  that the remainder can leverage that tool to great effect. But you have to understand how to
  work with the parsing system to do anything non-trivial.

  So, let's get started.")

(defcard-doc
  "
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

(defcard-doc
  "
  ## Implementing Read

  When building your application you must build a read function such that it can
  pull data from the client-side database that the parser needs to fill in the result
  of a query parse.

  The Om Next parser understands the grammar, and is written in such a way that the process
  is very simple:

  - The parser calls your `read` with the key that it parsed, along with some other helpful information.
  - Your read function returns a value for that key (possibly calling the parser recursively if it is a join).
  - The parser generates the result map by putting that key/value pair into
  the result at the correct position (relative to the query).

  Note that the parser only processes the query one level deep. Recursion (if you need it)
  is controlled by your read.
  ")

(defcard parser-read-trace
         "This card is similar to the prior card, but it has a read function that just records what keys it was
         triggered for. Give it an arbitrary legal query, and see what happens.

         Some interesting queries:

         - `[:a :b :c]`
         - `[:a {:b [:x :y]} :c]`
         - `[{:a {:b {:c [:x :y]}}}]`

         "
         (fn [state _]
           (let [{:keys [v error]} @state
                 trace (atom [])
                 read-tracking (fn [env k params]
                                 (swap! trace conj {:read-called-with-key k}))
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

(defcard-doc
  "
  In the card above you should have seen that only the top-level keys trigger reads.

  So, the query:

  ```clj
  [:kw {:j [:v]}]
  ```

  would result in a call to your read function on `:kw` and `:j`. Two calls. No
  automatic recursion. Done. The output value of the *parser* will be a map (that
  parse creates) which contains the keys (from the query, copied over by the
  parser) and values (obtained from your read):

  ```clj
  { :kw value-from-read-for-kw :j value-from-read-for-j }
  ```

  Note that if your read accidentally returns a scalar for `:j` then you've not
  done the right thing...a join like `{ :j [:k] }` expects a result that is a
  vector of (zero or more) things *or* a singleton *map* that contains key
  `:k`.

  ```clj
  { :kw 21 :j { :k 42 } }
  ; OR
  { :kw 21 :j [{ :k 42 } {:k 43}] }
  ```

  Dealing with recursive queries is a natural fit for a recusive algorithm, and it
  is perfectly fine to invoke the `parser` function to descend the query. In fact,
  the `parser` is passed as part of your environment.

  So, the read function you write will receive three arguments, as described below:

  1. An environment containing:
      + `:ast`: An abstract syntax *tree* for the element, which contains:
         + `:type`: The type of node (e.g. :prop, :join, etc.)
         + `:dispatch-key`: The keyword portion of the triggering query element (e.g. :people/by-id)
         + `:key`: The full key of the triggering query element (e.g. [:people/by-id 1])
         + `:query`: (same as the query in `env`)
         + `:children`: If this node has sub-queries, will be AST nodes for those
         + others...see documentation
      + `:parser`: The query parser
      + `:state`: The application state (an atom)
      + `:query`: **if** the element had one E.g. `{:people [:user/name]}` has `:query` `[:user/name]`
  2. A dispatch key for the item that triggered the read (same as dispatch key in the AST)
  3. Parameters (which are nil if not supplied in the query)

  It must return a value that has the shape implied the grammar element being read.

  So, lets try it out.
  ")

(defn read-42 [env key params] {:value 42})
(def parser-42 (om/parser {:read read-42}))

(defcard-doc
  "
  ### Reading a keyword

  If the parser encounters a keyword `:kw`, your function will be called with:

  ```clj
  (your-read
    { :state app-state :parser (fn ...) } ;; the environment. App state, parser, etc.
    :kw                                   ;; the keyword
    nil) ;; no parameters
  ```

  your read function should return some value that makes sense for
  that spot in the grammar. There are no real restrictions on what that data
  value has to be in this case. You are reading a simple property.
  There is no further shape implied by the grammar.
  It could be a string, number, Entity Object, JS Date, nil, etc.

  Due to additional features of the parser, *your return value must be wrapped in a
  map with the key `:value`*. If you fail to do this, you will get nothing
  in the result.

  Thus, a very simple read for props (keywords) could be:

  ```clj
  (defn read [env key params] { :value 42 })
  ```

  below is a devcard that implements exactly this `read` and plugs it into a
  parser like this:"
  (dc/mkdn-pprint-source read-42)
  (dc/mkdn-pprint-source parser-42)
  )

(defn parser-tester [parser]
  (fn [state _]
    (let [{:keys [v error]} @state]
      (dom/div nil
               (dom/input #js {:type     "text"
                               :value    v
                               :onChange (fn [evt] (swap! state assoc :v (.. evt -target -value)))})
               (dom/button #js {:onClick #(try
                                           (swap! state assoc :error "" :result (parser {:state (atom (:db @state))} (r/read-string v)))
                                           (catch js/Error e (swap! state assoc :error e))
                                           )} "Run Parser")
               (when error
                 (dom/div nil (str error)))
               (dom/h4 nil "Query Result")
               (html-edn (:result @state))
               (dom/h4 nil "Database")
               (html-edn (:db @state))
               ))))

(defcard property-read-for-the-meaning-of-life-the-universe-and-everything
         "This card is using the parser/read pairing shown above (the read returns
         the value 42 no matter what it is asked for). Run any query you
         want in it, and check out the answer.

         This card just runs `(parser-42 {:state {} } your-query)` and reports the result.

         Some examples to try:

         - `[:a :b :c]`
         - `[:what-is-6-x-7]`
         - `[{:a {:b {:c {:d [:e]}}}}]` (yes, there is only one answer)
         "
         (parser-tester parser-42)
         {:db {}}
         )

(defn property-read [{:keys [state]} key params] {:value (get @state key :not-found)})
(def property-parser (om/parser {:read property-read}))

(defcard-doc
  "
  So now you have a read function that returns the meaning of life the universe and
  everything in a single line of code! But now it is obvious that we need to build
  an even bigger machine to understand the question.

  If your app state is just a flat set of scalar values with unique keyword
  identities, then a better read is similarly trivial.

  The read function:
  "
  (dc/mkdn-pprint-source property-read)
  "
  Just assumes the property will be in the top-level of the app state atom.
  "
  )

(defcard trivial-property-reader
         "This card is using the `property-read` function above in a parser.

         The database itself is shown at the bottom of the card, after the
         result.

         Run some queries and see what you get. Some suggestions:

         - `[:a :b :c]`
         - `[:what-is-6-x-7]`
         - `[{:a {:b {:c {:d [:e]}}}}]` (yes, there is only one answer)
         "
         (parser-tester property-parser)
         {:db {:a 1 :b 2 :c 99}}
         {:inspect-data false}
         )

(def flat-app-state (atom {:a 1 :user/name "Sam" :c 99}))

(defn flat-state-read [{:keys [state parser query] :as env} key params]
  (if (= :user key)
    {:value (parser env query)}                             ; recursive call. query is now [:user/name]
    {:value (get @state key)}))                             ; gets called for :user/name :a and :c

(def my-parser (om/parser {:read flat-state-read}))

(defcard-doc
  "
  The result of those nested queries is supposed to be a nested map. So, obviously we
  have more work to do.

  ### Reading a join

  Your app state probably has some more structure to it than just a flat
  bag of properties. Joins are naturally recursive in syntax, and
  those that are accustomed to writing parsers probably already see the solution.

  First, let's clarify what the read function will receive for a join. When
  parsing:

  ```clj
  { :j [:a :b :c] }
  ```

  your read function will be called with:

  ```clj
  (your-read { :state state :parser (fn ...) :query [:a :b :c] } ; the environment
             :j                                                 ; keyword of the join
             nil) ; no parameters
  ```

  But just to prove a point about the separation of database format and
  query structure we'll implement this next example
  with a basic recursive parse, *but use more flat data* (the following is live code):

  "
  (dc/mkdn-pprint-source flat-app-state)
  (dc/mkdn-pprint-source flat-state-read)
  (dc/mkdn-pprint-source my-parser)
  "
  The important bit is the `then` part of the `if`. Return a value that is
  the recursive parse of the sub-query. Otherwise, we just look up the keyword
  in the state (which as you can see is a very flat map).

  The result of running this parser on the query shown is:

  "
  (my-parser {:state flat-app-state} '[:a {:user [:user/name]} :c])
  "

  The first (possibly surprising thing) is that your result includes a nested
  object. The parser creates the result, and the recusion natually nested the
  result correctly.

  Next you should remember that join implies a there could be one OR many results.
  The singleton case is fine (e.g. putting a single map there). If there are
  multiple results it should be a vector.

  In this case, we're just showing that you can use the parser recursively
  and it in turn will call your read function again.
  In a real application, your data will not be this flat, so you
  will almost certainly not do things in quite this
  way.

  So, let's put a little better state in our application, and write a
  more realistic parser.

  ### A Non-trivial, Recursive Example

  Let's start with the following hand-normalized application state:

  "
  (dc/mkdn-pprint-source parser1/app-state)
  "
  Our friend `db->tree` could handle queries against this database,
  but let's implement it by hand.

  Say we want to run this query:

  "
  (dc/mkdn-pprint-source parser1/query)
  "

  From the earlier discussion you see that we'll have to handle the
  top level keys one at a time.

  For this query there are only two keys to handle: `:friends`
  and `:window-size`. So, let's write a case for each:

  "
  (dc/mkdn-pprint-source parser1/read)
  "

  The default case is `nil`, which means if we supply an errant key in the query no
  exception will happen.

  when we run the query, we get:

  "
  (parser1/parser {:state parser1/app-state} parser1/query)
  "

  Those of you paying close attention will notice that we have yet to need
  recursion. We've also done something a bit naive: select-keys assumes
  that query contains only keys! What if app state and query were instead:

  "
  (dc/mkdn-pprint-source parser2/app-state)
  (dc/mkdn-pprint-source parser2/query)
  "

  Now things get interesting, and I'm sure more than one reader will have an
  opinion on how to proceed. My aim is to show that the parser can be called
  recursively to handle these things, not to find the perfect structure for the
  parser in general, so I'm going to do something simple.

  The primary trick I'm going to exploit is the fact that `env` is just a map, and
  that we can add stuff to it. When we are in the context of a person, we'll add
  `:person` to the environment, and pass that to a recursive call to `parser`.

  "
  (dc/mkdn-pprint-source parser2/read)
  "
  and running the query gives the expected result:

  "
  (parser2/parser {:state parser2/app-state} parser2/query)
  "

  All of the code shown here is being actively pulled (and run) from `om-tutorial.state-reads.parser-2`.

  Now, I feel compelled to mention a few things:

  - Keeping track of where you are in the parse (e.g. person can be generalized to
  'the current thing I'm working on') allows you to generalize this algorithm.
  - `db->tree` can still do everything we've done so far.
  - If you fully generalize the property and join parsing, you'll essentially recreate
  `db->tree`.

  So now you should be trying to remember why we're doing all of this. So let's talk
  about a case that `db->tree` can't handle: parameters.

  ## Parameters

  In the query grammar most kinds of rules accept parameters. These are intended
  to be combined with dynamic queries that will allow your UI to have some control
  over what you want to read from the application state (think filtering, pagination,
  sorting, and such).

  As you might expect, the parameters on a particular expression in the query
  are just passed into your read function as the third argument.
  You are responsible for both defining and interpreting them.
  They have no rules other than they are maps.

  To read the property `:load/start-time` with a parameter indicating a particular
  time unit you might use:

  ```clj
  [(:load/start-time {:units :seconds})]
  ```

  this will invoke read with:

  ```clj
  (your-read env :load/start-time { :units :seconds})
  ```

  the implication is clear. The code is up to you. Let's add some quick support for this
  in our read (in om-tutorial.state-reads.parser-3):
  "
  (dc/mkdn-pprint-source parser3/app-state)
  (dc/mkdn-pprint-source parser3/read)
  (dc/mkdn-pprint-source parser3/parser)
  "Now we can try the following queries:"
  (dc/mkdn-pprint-source parser3/parse-result-mins)
  parser3/parse-result-mins
  (dc/mkdn-pprint-source parser3/parse-result-secs)
  parser3/parse-result-secs
  (dc/mkdn-pprint-source parser3/parse-result-ms)
  parser3/parse-result-ms
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
           (om/db->tree '[[:widget/people _]] app-state app-state)))

           ; results in:

            {:widget/people [{:db/id 1, :person/name \"Sam\", :person/mate [:people/by-id 2]}
                             {:db/id 2, :person/name \"Jenny\", :person/mate [:people/by-id 1]}]}
           ```
           "
           (is (= {:widget/people [sam jenny]} (om/db->tree '[[:widget/people _]] app-state app-state)))
           "- You can read a simple ident query with full ID:

           ```
           (om/db->tree '[[:people/by-id 2]] app-state app-state)

            ; gives

            {[:people/by-id 2] {:db/id 2, :person/name \"Jenny\", :person/mate [:people/by-id 1]}}
           ```
           "
           (is (= {[:people/by-id 2] jenny} (om/db->tree '[[:people/by-id 2]] app-state app-state)))
           "- You can use an ident as the key to a join, and follow the graph to generate more of a tree:

           ```
           (om/db->tree '[{[:people/by-id 1] [:person/name {:person/mate [:person/name]}]}] app-state app-state)

            ; gives

            {[:people/by-id 1] {:person/name \"Sam\", :person/mate {:person/name \"Jenny\"}}}
           ```
           "
           (is (= {[:people/by-id 1] {:person/name "Sam", :person/mate {:person/name "Jenny"}}}
                  (om/db->tree '[{[:people/by-id 1] [:person/name {:person/mate [:person/name]}]}] app-state app-state)))

           ))
