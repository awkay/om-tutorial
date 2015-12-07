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
  with interpreting the query/mutation grammar.

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

  (the indentation of this outline is rendered incorrectly. I sent a patch for devcards,
  but it has not been released...jump into `src/tutorial/om-tutorial/E-State-Reads-and-Parsing`
  and read the source):

  1. An environment containing:
      - `:ast`: An abstract syntax *tree* for the element, which contains:
         - `:type`: The type of node (e.g. :prop, :join, etc.)
         - `:dispatch-key`: The keyword portion of the triggering query element (e.g. :people/by-id)
         - `:key`: The full key of the triggering query element (e.g. [:people/by-id 1])
         - `:query`: (same as the query in `env`)
         - `:children`: If this node has sub-queries, will be AST nodes for those
         - others...see documentation
      - `:parser`: The query parser
      - `:state`: The application state (an atom)
      - `:query`: **if** the element had one E.g. `{:people [:user/name]}` has `:query` `[:user/name]`
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
  with a basic recursive parse, but use more flat data:

  ```clj
  (def app-state (atom {:a 1 :user/name \"Sam\" :c 99}))

  (defn read [{:keys [state parser query] :as env} key params]
    (if (= :user key)
      {:value (parser env query)} ; recursive call. query is now [:user/name]
      {:value (get @state key)})) ; gets called for :user/name :a and :c

  (def my-parser (om/parser {:read read}))
  (my-parser {:state app-state} '[:a {:user [:user/name]} :c])
  ```

  The important bit is the `then` part of the `if`. Return a value that is
  the recursive parse of the sub-query. Otherwise, we just look up the keyword
  in the state (which as you can see is a very flat map).

  The result of running this parser on the query shown is:

  ```clj
  {:a 1, :user {:user/name \"Sam\"}, :c 99}
  ```

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

TODO: REWRITE THIS AS A WALK THROUGH CONSTRUCTING THE PARSING HELPER FUNCTIONS.

  Let's start with the following hand-normalized application state. Note that
  I'm not using the query grammar for object references (which take the
  form [:kw id]). Writing a more complex parser will benefit from doing
  so, but it's our data and we can do what we want to!

  ```clj
  (def app-state (atom {
      :window/size [1920 1200]
      :friends #{1 3} ; these are people IDs...see map below for the objects themselves
      :people/by-id {
              1 { :id 1 :name \"Sally\" :age 22 :married false }
              2 { :id 2 :name \"Joe\" :age 22 :married false }
              3 { :id 3 :name \"Paul\" :age 22 :married true }
              4 { :id 4 :name \"Mary\" :age 22 :married false } }
       }))
  ```

  now we want to be able to write the following query:

  ```clj
  (def query [:window/size {:friends [:name :married]}])
  ```

  Here is where multi-methods start to come in handy. Let's use
  one:

  ```clj
  (defmulti rread om/dispatch) ; dispatch by key
  (defmethod rread :default [{:keys [state]} key params] nil)
  ```

  The `om/dispatch` literally means dispatch by the `key` parameter.
  We also define a default method, so that if we fail we'll get
  an error message in our console, but the parse will continue
  (returning nil from a read elides that key in the result).

  Now we can do the easy case: If we see something ask for window size:

  ```clj
  (defmethod rread :window/size [{:keys [state]} key params] {:value (get @state :window/size)})
  ```

  Bingo! We've got part of our parser. Try it out:

  ```clj
  (def my-parser (om/parser {:read rread}))
  (my-parser {:state app-state} query)
  ```

  and you should see:

  ```clj
  {:window/size [1920 1200]}
  ```

  The join result (`friends`) is elided because our default `rread` got called and
  returned `nil` (no results). OK, let's fix that:

  ```clj
  (defmethod rread :friends [{:keys [state query parser path]} key params]
        (let [friend-ids (get @state :friends)
              get-friend (fn [id] (get-in @state [:people/by-id id]))
              friends (mapv get-friend friend-ids)]
          {:value friends}
          )
        )
  ```

  when you run the query now, you should see:

  ```clj
  {:window/size [1920 1200],
   :friends [{:id 1, :name \"Sally\", :age 22, :married false}
             {:id 3, :name \"Paul\", :age 22, :married true, :married-to 2}]}
  ```

  Looks *mostly* right...but we only asked for `:name` and `:married`. Your
  read function is responsible for the value, and we ignored the query!

  This is pretty easy to remedy with the standard `select-keys` function. Change
  the get-friend embedded function to:

  ```clj
  get-friend (fn [id] (select-keys (get-in @state [:people/by-id id]) query))
  ```

  and now you've satisfied the query:

  ```clj
  {:window/size [1920 1200],
   :friends [{:name \"Sally\", :married false}
             {:name \"Paul\", :married true}]}
  ```

  Those of you paying close attention will notice that we have yet to need
  recursion. We've also done something a bit naive: select-keys assumes
  that query contains only keys! What if our query were instead:

  ```clj
  (def query [:window/size
              {:friends [:name :married {:married-to [:name]} ]}])
  ```

  Now things get interesting, and I'm sure more than one reader will have an
  opinion on how to proceed. My aim is to show that the parser can be called
  recursively to handle these things, not to find the perfect structure for the
  parser in general, so I'm going to do something simple.

  The primary trick I'm going to exploit is the fact that `env` is just a map, and
  that we can add stuff to it. When we are in the context of a person, we'll add
  `:person` to the environment, and pass that to `parser`. This makes parsing a query
  like `[:name :age]` as trivial as:

  ```clj
  (defmethod rread :name [{:keys [person]} key _] {:value (get person key)})
  (defmethod rread :age [{:keys [person]} key _] {:value (get person key)})
  (defmethod rread :married [{:keys [person]} key _] {:value (get person key)})

  (defmethod rread :friends [{:keys [state query parser path] :as env} key params]
    (let [friend-ids (get @state :friends)
          get-person (fn [id]
                       (let [raw-person (get-in @state [:people/by-id id])
                             env' (dissoc env :query) ; clear the parent query
                             env-with-person (assoc env' :person raw-person)]
                         (parser env-with-person query)
                         ))
          friends (mapv get-person friend-ids)]
      {:value friends}
      )
    )
  ```

  The three important bits:

  - We need to remove the :query from the environment, otherwise our nested
    read function will get the old query on plain keywords, making it
    impossible to tell if the parser saw `[:married-to]` vs. `{ :married-to [...] }`.
  - For convenience, we add `:person` to the environment.
  - The `rread` for plain scalars (like `:name`) are now trivial...just look on the
    person in the environment!

  The final piece is hopefully pretty transparent at this point.  For
  `:married-to`, we have two possibilities: it is queried as a raw value
  `[:married-to]` or it is joined `{ :married-to [:attrs] }`. By clearing the
  `query` in the `:friends` `rread`, we can tell the difference (since `parser`
  will add back a query if it parses a join).

  So, our final bit of this parser could be:

  ```clj
  (defmethod rread :married-to
    [{:keys [state person parser query] :as env} key params]
    (let [partner-id (:married-to person)]
      (cond
        (and query partner-id) { :value [(select-keys (get-in @state [:people/by-id partner-id]) query)]}
        :else {:value partner-id}
        )))
  ```

  If further recursion is to be supported on this query, then rinse and repeat.

  For those who read to the end first, here is an overall runnable segment of code
  for this parser:

  ```clj
  (def app-state (atom {
                        :window/size  [1920 1200]
                        :friends      #{1 3} ; these are people IDs...see map below for the objects themselves
                        :people/by-id {
                                       1 {:id 1 :name \"Sally\" :age 22 :married false}
                                       2 {:id 2 :name \"Joe\" :age 22 :married false}
                                       3 {:id 3 :name \"Paul\" :age 22 :married true :married-to 2}
                                       4 {:id 4 :name \"Mary\" :age 22 :married false}}
                        }))

  (def query-props [:window/size {:friends [:name :married :married-to]}])
  (def query-joined [:window/size {:friends [:name :married {:married-to [:name]}]}])

  (defmulti rread om/dispatch)

  (defmethod rread :default [{:keys [state]} key params] (println \"YOU MISSED \" key) nil)

  (defmethod rread :window/size [{:keys [state]} key params] {:value (get @state :window/size)})

  (defmethod rread :name [{:keys [person query]} key params] {:value (get person key)})
  (defmethod rread :age [{:keys [person query]} key params] {:value (get person key)})
  (defmethod rread :married [{:keys [person query]} key params] {:value (get person key)})

  (defmethod rread :married-to
    ;; person is placed in env by rread :friends
    [{:keys [state person parser query] :as env} key params]
    (let [partner-id (:married-to person)]
      (cond
        (and query partner-id) {:value [(select-keys (get-in @state [:people/by-id partner-id]) query)]}
        :else {:value partner-id}
        )))

  (defmethod rread :friends [{:keys [state query parser path] :as env} key params]
    (let [friend-ids (get @state :friends)
          keywords (filter keyword? query)
          joins (filter map? query)
          get-person (fn [id]
                       (let [raw-person (get-in @state [:people/by-id id])
                             env' (dissoc env :query)
                             env-with-person (assoc env' :person raw-person)]
                         ;; recursively call parser w/modified env
                         (parser env-with-person query)
                         ))
          friends (mapv get-person friend-ids)]
      {:value friends}
      )
    )

  (def my-parser (om/parser {:read rread}))

  ;; remember to add a require for cljs.pprint to your namespace
  (cljs.pprint/pprint (my-parser {:state app-state} query-props))
  (cljs.pprint/pprint (my-parser {:state app-state} query-joined))
  ```

  ## Parameters

  In the query grammar most kinds of rules accept parameters. These are intended
  to be combined with dynamic queries that will allow your UI to have some control
  over what you want to read from the application state (think filtering, pagination,
   and such).

  Remember that Om Next has a story for integrating with server communications,
  and these remote queries are meant to be transparent (from the UI perspective).
  If the UI needs less data parameters and query details can fine-tune what gets
  transferred over the wire.

  As you might expect, the parameters are just passed into your read function as
  the third argument. You are responsible for both defining and interpreting them.
  They have no rules other than they are maps:

  ```clj
  [(:load/start-time {:locale \"es-MX\" })]                ;;prop + params
  ```

  invokes read with:

  ```clj
  (your-read env :load/start-time { :locale \"es-MX\" })
  ```

  the implication is clear. The code is up to you.
  ")
(defn read-person [env dispatch-key params]
  (case dispatch-key
    :name {:value "Sally"}                                  ; important...wrap real result values in a map with key :value
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


