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

