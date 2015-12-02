(ns om-tutorial.local-read
  (:require [om-tutorial.parsing :as p]
            [om.next :as om]))

(defn read-local
  "The function used by our Om parser to read local app state."
  [{:keys [query ast state] :as env} key params]
  (case key
    :new-person {:value (get @state key)}
    :last-error {:value (get @state key)}
    (if (om/ident? (:key ast))
      {:value (get-in @state (:key ast))}
      {:value (om/db->tree query (get @state key) @state)}
      )))

