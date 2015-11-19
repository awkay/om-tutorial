(ns om-tutorial.parsing)

(defn new-read-entry-point
  "Create a new read entry point that allows you to switch read functions during a local parse, and can
  be configured with an alternate reader for each remote.
  
  The remote-read-map should be a map keyed by remote name, whose values are read functions 
  (e.g. `{:remote (fn [e k p] ...)}`)
  "
  [default-local-read remote-read-map]
  (fn [{:keys [target state reader] :as env} key params]
    (let [env' (if (:node env) env (assoc env :node @state))]
      (cond
        reader (reader env' key params)
        (contains? remote-read-map target) ((get remote-read-map target) env' key params)
        :else (default-local-read env' key params)
        ))))

(defn descend
  "Recursively descend the state database (moves :node in the env to the child with the given key in the current node (which originally
  starts at app-state root). Basically, walk into the app state. Assumes the shape of the state at the present node mimics
  the UI tree at that key.
  "
  [{:keys [node] :as env} key] (assoc env :node (get node key)))

(defn parse-with-reader
  "Cause this parse to recursively descend, but switch to using the named reader function. 
  
  `reader`: The reader function to start calling from parser
  `env`:The current parse environment
  `key`:The key just hit
  `descend?` : A truthy value (e.g. :descend). Should this function move env :node into the UI state at the given key?
  "
  [reader {:keys [parser query] :as env} key descend?]
  (let [env' (cond-> (assoc env :reader reader)
                     descend? (descend key)
                     )]
    (parser env' query)))

(defn parse-many-with-reader
  "
  "
  [reader {:keys [parser node query] :as env} key]
  (let [items (get node key)
        env' (-> env
                 (assoc :reader reader)
                 (dissoc :query))
        ]
    (when (or (nil? items) (= :missing items) (vector? items)) "Parsing expected a vector, but found none.")
    (mapv #(parser (assoc env' :node %) query) items)
    )
  )

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
  
  NOTE: If the *query* has a target on the AST (which indicates a forced read via a quote on the keyword) then this 
  function will always honor that and include the element in the remote fetch query.
   
  If the as-root? parameter is `true` or `:make-root`, then it indicates that the sub-query should be sent to the
  server as a root query, without any of the prior recursion's query prefix. If you use it, then you will also need
  to `process-roots` in your send method. Any other value can be used to indicate it is not a root.
  "
  [{:keys [state target ast] :as env} key as-root?]
  (let [cached-read-ok? (not (= target (:target ast)))
        value (get @state key)
        as-root? (or (true? as-root?) (= :make-root as-root?))
        ]
    (if (and cached-read-ok? (contains? @state key) (not= nil value) (not= :missing value))
      nil
      {target
       (cond-> ast
               as-root? (assoc :query/root true)
               )}
      )))


