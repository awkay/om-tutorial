(ns om-tutorial.D-Queries
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.next.impl.parser :as p]
            [om.dom :as dom]
            [cljs.reader :as r]
            [om-tutorial.queries.query-demo :as qd]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "
  # Om Queries

  ## Important Notes

  First, please understand that *Om does not know how to read your data* on the client *or* the server.
  It does provide some useful utilities for the default database format described in the App
  Database section, but since you can structure things in that format in some arbitrary way
  you do have to participate in query result construction. Make sure you've read the App
  Database section carefully, as we'll be leveraging that understanding here.

  That said, let's understand the query syntax and semantics.

  ## Query Syntax

  Queries are written with a variant of Datomic pull syntax.

  For reference, here are the defined grammar elements:
  ```clj
  [:some/key]                              ;;prop
  [(:some/key {:arg :foo})]                ;;prop + params
  [{:some/key [:sub/key]}]                 ;;join + sub-select
  [:some/k1 :some/k2 {:some/k3 ...}]       ;;recursive join
  [({:some/key [:sub/key]} {:arg :foo})]   ;;join + params
  [[:foo/by-id 0]]                         ;;reference
  [(fire-missiles!)]                       ;;mutation
  [(fire-missiles! {:target :foo})]        ;;mutation + params
  { :photo [...subquery...]
    :video [...subquery...]
    :comment [...subquery...] }             ;;union
  ```

  *RECOMMENDATAION*: Even if you do not plan to use Datomic, I highly recommend
  going through the [Datomic Pull Tutorial](http://docs.datomic.com/pull.html).
  It will really help you with Om Next queries.

  ## A quick note on Quoting

  Quoting is not an Om thing, it is a clj(s) thing. The syntax of Om queries is just data, but it
  uses things (like symbols and lists) that the compiler would like to give different meaning to than
  we need.

  If you have not done much in the way of macro programming you may be confused by the
  quoting often seen in these queries. I'd recommend doing a tutorial on macros, but
  here are some quick notes:

  - Using `'` quotes the form that follows it, making it literal.
  - Using a backquote in front of a form is a syntax quote. It will namespace all symbols, and allows
    you to unquote subforms using `~`. You'll sometimes also see ~' in front of a non-namespaced
    symbol *within* a syntax quoted form. This prevents a namespace from being added (unquote a form
    that literally quotes the symbol).

  ## Understanding Queries

  Except for unions, queries are represented as vectors. Each item in the vector is a request for a data item, or
  is a call to an abstract operation. Data items can indicate joins by nesting the given property name
  in a map.

  Let's start with a very simple database and query as shown in the card below:"
  )

(defn run-query [db q]
  (try
    (om/db->tree (r/read-string q) db db)
    (catch js/Error e "Invalid Query")))

(defcard query-example-1
         "This query asks for a person's name from the database. You can see our database (:db in the map below)
         has a bunch of top level properties...the entire database is just a single person.
         Play with the query. Ask for this person's age and database ID.

         Notes:

         - The query has to be a single vector
         - The result is a map, with keys that match the selectors in the query.
         "
         (fn [state-atom _]
           (dom/div nil
                    (dom/input #js {:type "text" :value (:query @state-atom)
                                    :size 80
                                    :onChange (fn [e] (swap! state-atom assoc :query (.. e -target -value)))})
                    (dom/button # js {:onClick #(swap! state-atom assoc :query-result (run-query (:db @state-atom) (:query @state-atom)))} "Run Query")
                    ))
         {:query "[:person/name]"
          :query-result {}
          :db {:db/id 1 :person/name "Sam" :person/age 23}}
         {:inspect-data true}
         )

(defcard-doc
  "
  A more interesting database has some tables in it, like we saw in the App Database section. Let's play with
  queries on one of those.")

(defcard query-example-2
         "This database (in :db below) has some performance statistics linked into a table and chart. Note that
         the query for the table is for the disk data, while the chart is using both. Play with the query a bit
         to make sure you understand it (e.g. erase it and try to write it from scratch).

         Note that the query result is a map in tree form. A tree is exactly what you need for a UI!
         "
         (fn [state-atom _]
           (dom/div nil
                    (dom/input #js {:type "text" :value (:query @state-atom)
                                    :size 120
                                    :onChange (fn [e] (swap! state-atom assoc :query (.. e -target -value)))})
                    (dom/button # js {:onClick #(swap! state-atom assoc :query-result (run-query (:db @state-atom) (:query @state-atom)))} "Run Query")
                    ))
         {:query        "[{:table [:name {:data [:disk-activity]}]}   {:chart [:name {:data [:disk-activity :cpu-usage]}]}]"
          :query-result {}
          :db           {:table      {:name "Disk Performance Table" :data [:statistics :performance]}
                         :chart      {:name "Combined Graph" :data [:statistics :performance]}
                         :statistics {:performance {
                                                    :cpu-usage     [45 15 32 11 66 44]
                                                    :disk-activity [11 34 66 12 99 100]
                                                    :network-activity [55 87 20 01 22 82]
                                                    }}}}
         {:inspect-data true}
         )


  (defcard-doc "
  ## Co-located Queries on Components

  After playing with the database and queries, you now see that query results from simple properties and joins
  through the graph can be used to generate an arbitrary tree of data for your UI. Now the next step is to
  localize the bits of query onto the components that need the related data.

  ### Relativity

  No, not that kind...

  Placing a query on a component declares what data that component needs in order to render correctly. Since
  components are little fragments of UI, the queries on them are little fragments as well (e.g. they are detached from anything
  specific until placed in a tree). Thus, a `Person` component might declare it needs the person's name, but you have
  yet to answer \"which one\":

  "
  (dc/mkdn-pprint-source qd/Person)
  "

  For this query to make sense it must be composed towards a (relative) root that will give enough context so that
  we can understand how to pull that data from the database.

  IMPORTANT NOTE: understand that queries *must* compose all the way to the root or your UI or Om won't find them!

  My \"relativity\" comment above is about the fact that the *context* of understanding the query bits is relative.

  Examples might be:

  Get people that are my friends (e.g. relative to your login cookie):

  ```
  [{:current-user/friends (om/get-query Person)}]
  ```

  Get people that work for the company (context of login):

  ```
  [{:company/active-employees (om/get-query Person)}]
  ```

  Get a very specific user (using an ident):

  ```
  [{[:user/by-id 42] (om/get-query Person)}]
  ```

  The `query-demo.cljs` contains components:

  "
  (dc/mkdn-pprint-source qd/Person)
  (dc/mkdn-pprint-source qd/person)
  (dc/mkdn-pprint-source qd/PeopleWidget)
  (dc/mkdn-pprint-source qd/people-list)
  (dc/mkdn-pprint-source qd/Root)
  "

  The above component make the following UI tree:

  TODO: UI Tree diagram

  But the queries form the following query tree:

  TODO: Query diagram

  because the middle component (PeopleWidget) does not have a query. Pay careful attention to how the queries are
  composed (among stateful components).

  So, this example will render correctly when the query result looks like what you see in the card below:
")

(defcard sample-rendering-with-result-data
         (fn [state _] (qd/root @state))
         {:people [{:db/id 1 :person/name "Joe"}
                   {:db/id 2 :person/name "Guy"}
                   {:db/id 3 :person/name "Tammy"}
                   ]}
         {:inspect-data true}
         )

(defcard-doc "
  In the above example, you could have just as well defined the intermediate node as a plain function, and saved
  yourself some typing (and the need to unpack props in the middle).

  You should edit this document (`C_Queries.cljs`) and play with the sample-rendering-with-result-data card. The
  code for the UI for this example is in `tutorial/om_tutorial/queries/query_demo.cljs`.

  ## More Advanced Queries

  ### Parameters

  All of the query elements can be parameterized. These parameters are passed down to the query engine (which you help
  write), and can be used however you choose. Om does not give any meaning whatsoever to these parameters. See
  the section on [State Reads and Parsing](#!/om_tutorial.E_State_Reads_and_Parsing) for more information.

  ### Looking up by Ident

  An Ident (or reference) is a vector with 2 elements. The first is a keyword, and the second is some kind of
  selector (e.g. numeric id). These can be used in place of a property name to indicate a specific instance
  of some object in the database. This provides explicit context from which the remainder of the query
  can be evaluated. This is a semantic meaning. You're going to be writing the portion of the query processing engine
  that understands what these refer to in the real application state.

  more TODO

  ### Union Queries

  When a component is showing a sequence of things, and each of those things might be different, then you need
  a union query. Basically, it is a *join*, but it names all of the alternative things that might appear
  in the resulting collection. Instead of being a vector, unions are maps of vectors (where each value in the map
  is the query for the keyed kind of thing).

  more TODO

  ## Common Mistakes

  ### Failing to Reach the UI Root

  Om only looks for the query on the root component of your UI! Make sure your queries compose all the way to
  the root! Basically the Root component ends up with one big fat query for the whole UI, but you get to
  *reason* about it through composition (recursive use of `get-query`). Also note that all of the data
  gets passed into the Root component, and every level of the UI that asked for (or composed in) data
  must pick that apart and pass it down. In other words, you can pretend like you UI doesn't even have
  queries when working on your render functions. E.g. you can build your UI, pick apart a pretend
  result, then later add queries and everything should work.

  ### Declaring a query that is not your own

  Beginners often make the mistake:

  ```
  (defui Widget
       static om/IQuery
       (query [this] (om/get-query OtherWidget))
       ...)
  ```

  because they think \"this component just needs what the child needs\". If that is truly the case, then
  Widget should not have a query at all (the parent should compose OtherWidget's into it's own query). The most common
  location where this happens is at the root, where you my not want any specific data yourself.

  In that case, you *do* need a stateful component, but you'll need to get the child data using a join, then
  pick it apart via code and manually pass those props down:

  ```
  (defui RootWidget
       static om/IQuery
       (query [this] [{:other (om/get-query OtherWidget)}])
       Object
       (render [this]
          (let [{:keys [other]} (om/props this)] (other-element other)))
  ```

  ### Making a component when a function would do

  Sometimes you're just trying to clean up code and factor bits out. Don't feel like you have to wrap UI code in
  `defui` if it doesn't need any support from React or Om. Just write a function! `PeopleWidget` earlier in this
  document is a great example of this.

  ### TODO
  ")

