(ns om-tutorial.local-read
  (:require [om-tutorial.parsing :as p]
            [om.next :as om]))

(defn read-local
  "The function used by our Om parser to read local app state."
  [env key params]
  (case key
    :ui/checked {:value (p/ui-attribute env key) }
    :person/mate {:value (p/parse-join-with-reader read-local env key :limit 2)}
    :people {:value (p/parse-join-with-reader read-local env key)}
    :widget {:value (p/parse-join-with-reader read-local env key :reset-depth 0)}
    (p/db-value env key)
    ))

(defn generic-read-local
  "EXPERIMENTAL: A function used Om parser to read local app state. This should work for everything except deep nesting
  and unions (not supported yet by my helpers). NOTE: This also supports using the special ui-attribute db store that
  I invented for decomplecting UI state from persistent state"
  [{:keys [ast] :as env} key params]
  (println ast)
  (let [is-ui? (= "ui" (namespace key))
        is-join? (and (= :prop (:type ast)) (:query ast))]
    (cond
      is-ui? {:value (p/ui-attribute env key)}
      is-join? {:value (p/parse-join-with-reader generic-read-local env key :limit 10)}
      :else (p/db-value env key)
      )))
