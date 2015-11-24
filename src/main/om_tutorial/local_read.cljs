(ns om-tutorial.local-read
  (:require [om-tutorial.parsing :as p]
            [om.next :as om]))

(defn read-local
  "The function used by our Om parser to read local app state."
  [{:keys [query ast db-path] :as env} key params]
  (case key
    :people/by-id {:value (p/parse-join-with-reader read-local (assoc env :db-path []) (:key ast))}
    :ui.people/checked (p/ui-attribute env key)
    ;; needed to bump recursion limit due to possible path optimization path starting point
    :person/mate (when-not (:mate-read env) {:value (p/parse-join-with-reader read-local (assoc env :mate-read true) key)})
    :people {:value (p/parse-join-with-reader read-local env key)}
    :widget {:value (p/parse-join-with-reader read-local env key)}
    (p/db-value env key)
    ))


(comment
  (defn generic-read-local
    "EXPERIMENTAL: A function used by an Om parser to read local app state. This should work for lots of things except deep nesting
    and unions (not supported yet by my helpers). NOTE: This also supports using the special ui-attribute db store that
    I invented for decomplecting UI state from persistent state"
    [{:keys [ast] :as env} key params]
    (println "READ " key)
    (case key
      ; path optimization...TODO: generalize
      :people/by-id {:value (p/parse-join-with-reader read-local (assoc env :db-path []) (:key ast))}
      (let [is-ui? (and (string? (namespace key)) (re-matches #"^ui\..*" (namespace key)))
            is-join? (= :join (:type ast))]
        (cond
          is-ui? (p/ui-attribute env key)
          is-join? {:value (p/parse-join-with-reader generic-read-local env key :limit 3)}
          :else (p/db-value env key)
          ))))

  ;; NOTE: The following versions have not been updated to support pathopt
  (defn split-read-person "Read stuff for a person query." [env key params]
    (case key
      :ui/checked (p/ui-attribute env key)
      :person/mate {:value (p/parse-join-with-reader split-read-person env key :limit 2)}
      (p/db-value env key)
      ))

  (defn split-read-widget "Read stuff for the widget" [env key params]
    (case key
      :people {:value (p/parse-join-with-reader split-read-person env key)}
      :not-found
      ))

  (defn split-read-local
    "A version of read-local that splits the responsibilities among other functions."
    [env key params]
    (case key
      :widget {:value (p/parse-join-with-reader split-read-widget env key :reset-depth 0)}
      (p/db-value env key)
      )))
