(ns om-tutorial.C-Queries
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [om-tutorial.queries.query-demo :as qd]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "
  # Queries

  First, please understand that *Om does not know how to read your data* on the client *or* the server.
  Om provides a query syntax and a query parser, but you end up providing the \"data retrievale\" part
  of the works.

  That said, let's understand the query syntax and semantics.
  
  ## Syntax
  
  Queries are written with a variant of Datomic pull syntax.
  
  For reference, here are the defined grammar elements:
  ```clj
  [:some/key]                              ;;prop
  [(:some/key {:arg :foo})]                ;;prop + params
  [{:some/key [:sub/key]}]                 ;;join + sub-select
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
  
  ## Understanding Queries
  
  Except for unions, queries are represented as vectors. Each item in the vector is a request for a data item, or
  is a call to an abstract operation. Data items can indicate joins by nesting the given property name
  in a map. The most common query looks like this:
  
  ```
  [:person/id :person/name {:person/occupation [:occupation/name]}]
  ```
  
  The above query indicates you'd like to know a person's name, id, and details about their occupation. This
  implies that `:person/occupation` is a to-one or to-many relationship. The result of a query is a map, 
  filled in (recursively) with the data requested. For example, the two possible structures for the above
  query are:
  
  ```
  { :person/id 1
    :person/name \"Ben\"
    :person/occupation { :occupation/name \"UI Designer\" } }
  ```
  
  for a person with one job, or possibly
  
  ```
  { :person/id 1
    :person/name \"Ben\"
    :person/occupation [{ :occupation/name \"UI Designer\" } { :occupation/name \"Pilot\" }] }
  ```
  
  for someone with two. Note that the join ended up placing the join key on the top-level object, which is 
  naturally where it would belong in data. The map in the query is about specifying what \"colums\" you want
  out of the resulting joined object(s). If you had asked:
  
  ```
  [:person/id :person/name {:person/occupation [:occupation/name :occupation/avg-pay]}]
  ```
  
  you might have gotten:
  
  ```
  { :person/id 1
    :person/name \"Ben\"
    :person/occupation { :occupation/name \"UI Designer\" :occupation/avg-pay 60000.0 } }
  ```
  
  ## Co-located Queries on Components

  ### Relativity

  No, not that kind...

  Placing a query on a component declares what data that component needs in order to render correctly. Since
  components are little fragments of UI, the queries on them are little fragments as well (e.g. they are detached from anything
  specific until placed in a tree). Thus, a `Person` component might declare it needs the person's name, but you have
  yet to answer \"which one\":

  "
  (dc/mkdn-pprint-source qd/Person)
  "

  As such, such a \"leaf\" must be composed towards a (relative) root that will give enough context so that the database reading
  code can determine what to feed that component.

  IMPORTANT NOTE: understand that queries *must* compose all the way to the root or your UI or Om can't find them!
  My relative comment above is about the fact that the *context of understanding* the query bits is relative.

  Examples might be:

  Get people that are my friends (e.g. relative to your login cookie):

  ```
  [{:current-user/friends (om/get-query Person)}]
  ```

  Get people that work for the company (context of login):

  ```
  [{:company/active-employees (om/get-query Person)}]
  ```

  Get a very specific user (using an ident, which is explained later..basically it is a way to name a specific
  bit of data uniquely):

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



  TODO:
  
  - Quoting
  - Composition of queries
  - Stateless components in UI tree
  - UI Tree vs Query Tree
    
  Once you've understood the above, you might want to proceed to the section on the [App Database](#!/om_tutorial.D_App_Database).
  
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
  the root!

  ### Declaring a query that is not your own

  Beginners often make the mistake:

  ```
  (defui Widget
       static om/IQuery
       (query [this] (om/get-query OtherWidget))
       ...)
  ```

  because they think \"this component just needs what the child needs\". If that is truly the case, then
  Widget should not have a query at all (the parent should compose it into it's own query). The most common
  location where this happens is at the root, where you my not want any specific data yourself.

  In that case, you *do* need a stateful component, but you'll need to get the child data using a join, then
  pick it apart via code and manually pass those props down:

  ```
  (defui Widget
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
    
