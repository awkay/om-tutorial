(ns om-tutorial.parsing)

(defn parse-with-reader
  "Cause this parse to recursively descend, but switch to using the named reader function. 
  
  `reader`: The reader function to start calling from parser
  `env`:The current parse environment
  `key`:The key just hit
  "
  [reader {:keys [parser query] :as env} key]
  (let [env' (assoc env :reader reader)] (parser env' query)))

(defn elide-empty-response
  "Prevent a remote request if the response from a sub-parser is empty."
  [target {:keys [query] :as response}]
  (if (or (nil? query) (empty? query))
    nil
    {target response})
  )

(defn recurse-remote
  "Recursively calls the parser on the subquery which will completely determine the result of
  whether or not a remote request is needed. Call this when you want to delegate the remote 
  response decision to the sub-query."
  [{:keys [target ast parser] :as env} key]
  (elide-empty-response target (update-in ast [:query] #(parser env % target))))


(defn fetch-if-missing
  "Terminate the descent of the parse and indicate the rest of the sub-query should run against the target indicated
  by the environment *if* the given key in the current app-state node is missing, nil, or `:missing`. 
  
  NOTE: If the *query* has a target on the AST (which indicates a forced read) then this function will always honor that
  and run the remote fetch.
   
  The as-root? parameter indicates that the sub-query should be sent to the
  server as a root query, without any of the prior recursion's query prefix. If you use it, then you will also need
  to `process-roots` in your send method.
  "
  [{:keys [node target ast] :as env} key as-root?]
  (let [cached-read-ok? (not (= target (:target ast)))
        value (get node key)
        ]
    (if (and cached-read-ok? (contains? node key) (not= nil value) (not= :missing value))
      nil
      {target
       (cond-> ast
               as-root? (assoc :query/root true)
               )}
      )))


