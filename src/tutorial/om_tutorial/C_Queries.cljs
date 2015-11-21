(ns om-tutorial.C-Queries
  (:require-macros
    [cljs.test :refer [is]]
    )
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            ))

(defcard-doc
  "
  # Queries
  
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
  
  TODO:
  
  - Quoting
  - Composition of queries
  - Stateless components in UI tree
  - UI Tree vs Query Tree
    
  Once you've understood the above, you might want to proceed to the section on the [App Database](#!/om_tutorial.D_App_Database).
  
  ## More Advanced Queries
  
  TODO
  ")
    
