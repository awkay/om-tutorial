(ns om-tutorial.C-App-Database
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            [cljs.reader :as r]))

(defcard-doc
  "
  # App Database

  In this section we'll discuss the default database format used by Om for client state. The
  database is completely pluggable (some people are using Datascript, for example), but the
  default format solves the problem nicely, and uses very fast data structures.

  First, we'll describe the problem, and then show how Om's approach to storing app state solves it.

  ## The Problem

  Any non-trivial UI needs data. Many non-trivial UIs need a lot of data. React UIs need data to
  be in a tree-like form (parents pass properties down to children). When you combine these facts
  with Clojurescript and immutabe data structures you end up with some interesting
  challenges.

  The most important one is this: What do I do when I want to show the same information in
  two different UI components (e.g. a Table and a Chart of some performance statistics)?

  If you structure your data as a tree (for UI benefit), then you have to duplicate the
  data across the tree. Note that now what you have is really a graph.

  TODO: diagram of graph described above (root -> (table,chart), table -> perf stats, chart -> perf stats)

  You might think this is OK (structural sharing and pointers make this
  nearly free) until you consider what happens next: time passes. The data needs to update.

  In a mutable world, you'd just update the data in-place, and the pointers would now point
  to that new state. If you're reading this you've already learned the perils and disadvantages of *that*.

  So, now you have the lovely task of finding all of that data in the application state and updating
  it to the new data set to produce your new (immutable) application state. This turns a localized
  concern (updating the data for a table) into a global one (what is using *this* bit of state in my
  global application state?).

  ## The Solution

  Experienced readers will recognize that the solution is the one we've been using in databases
  for quite a long time: normalization...de-dupe the data!

  In Om's default database format we do not place the *real* data in multiple places, but instead
  use a special bit of data that acts like a database foreign key. Om calls these *idents*.

  Here's how it works:

  Create a map. This is your application state.

  For any given piece of data that you wish to easily share (or access), invent an identifier for it
  of the form `[keyword id]`, where id can be anything (e.g. keyword, int, etc). The explicit
  requirements are:

  - The first item in the ident vector *must* be a keyword
  - The vector *must* have exactly two items.

  Now place the real information into the map as follows:

  ```
  (def app-state { :keyword { id real-information }})
  ```

  Notice that what you've now created is a location to store data that can be trivially accessed via
  `get-in` and the *ident*:

  ```
  (get-in app-state [:keyword id])
  ```

  So, now we can represent our earlier example graph as:

  ```
  { :table [:data/statistics :performance]
    :chart [:data/statistics :performance]
    :data/statistics { :performance { ... actual stats ... }}}
  ```

  Note also that the objects stored this way are also encouraged to use idents to
  reference other state. So you could build a database of people are their partners like this:

  ```
  { :list/people [ [:people/by-id 1] [:people/by-id 2] ... ]
    :people/by-id { 1 { :db/id 1 :person/name \"Joe\" :person/mate [:people/by-id 2]}
                    2 { :db/id 2 :person/name \"Sally\" :person/mate [:people/by-id 1]}}}
  ```

  The top-level key `:list/people` is a made-up keyword for my list of people that
  I'm interested in (e.g. currently on the UI). It points to Joe and Sally.

  The database table keyed at `:people/by-id` stores the real object, which cross-reference each
  other. Note that this particular graph (as you might expect) has a loop in it (Joe is married
  to Sally who is married to Joe ...).

  ## Everything in Tables

  By now you might have realized that you can just put just about everything into this table format.

  For example if I have mutliple different lists of people I might choose to store
  *those* in more of a table format:

  ```
  { :lists/by-category { :friends { :list/id :friends :people [ [:people/by-id 1] [:people/by-id 2] ] }
                         :enemies { :list/id :enemies :people [ [:people/by-id 5] [:people/by-id 9] ] }}
    :people/by-id { 1 { :db/id 1 :person/name \"Joe\" :person/mate [:people/by-id 2]}
                    2 { :db/id 2 :person/name \"Sally\" :person/mate [:people/by-id 1]} ... }}

  ```

  This will work very well.

  ## Some things not in tables?

  In practice, many things in you UI are really singletons. So, in practice it often makes
  perfect sense to just store those things in the top level of your overall application state,
  or even as a simple tree.

  One criteria you might consider before placing data into a tree is changing it over time (in
  value or location). If you nest some bit of state way down in a tree and need to update
  that state, you'll end up writing code that is tied to that tree structure. For example:
  `(update-in state [:root :list :wrapper-widget :friends] conj new-friend)`. Not only
  is this painful to write, it ties a local UI concern into your state management code
  and starts to look like a controller from MVC. It also means that if you write a different
  (e.g. mobile) UI, you won't easily re-use that bit of code.

  Not everything has to go into an ident-compatible table format, but avoid nesting ui-concerns
  into your application state.

  In fact, Om has great support for true UI singletons in queries (as we'll see). So if you
  have this kind of data just use (namespaced) keywords at the top level:

  ```
  { :modal/error { :message \"There was an error\" }
    :friend-list [ [:people/by-id 1] ]
    ...
  }
  ```

  In general, keep your application state flat. The graph nature fixes duplication issues,
  and the flat structure makes mutation code easy to write and maintain.
  "
  )


