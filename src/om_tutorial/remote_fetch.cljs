(ns om-tutorial.remote-fetch
  (:require [om-tutorial.parsing :as p]))

(defn read-remote
  "The read function used by Om parsing to determine if a remote load should happen for given data.
  
  The helper functions `recurse-remote` and `fetch-if-missing` take a good deal of the complexity out.
  "
  [env key params]
  (case key
    :widget (p/recurse-remote env key)
    :people (p/fetch-if-missing env key true)
    :not-remote
    )
  )
