(ns om-tutorial.parsing
  (:require [om.next :as om]))

(defn new-read-entry-point
  "Create a new read entry point that allows you to switch read functions during a local parse, and can
  be configured with an alternate reader for each remote.
  
  The remote-read-map should be a map keyed by remote name, whose values are read functions 
  (e.g. `{:remote (fn [e k p] ...)}`)
  "
  [default-local-read remote-read-map]
  (fn [{:keys [target state reader] :as env} key params]
    (let [env' (if (contains? env :db-path) env (assoc env :db-path []))]
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
  [env key]
  (update-in env [:db-path] conj key))

(defn parse-with-reader
  "Cause this parse to recursively descend, but switch to using the named reader function. 
  
  `reader`: The reader function to start calling from parser
  `env`:The current parse environment
  `key`:The key just hit
  `descend?` : A truthy value (e.g. :descend). Should this function move env's :db-path for this key?
  "
  [reader {:keys [parser query] :as env} key descend?]
  (let [env' (cond-> (assoc env :reader reader)
                     descend? (descend key)
                     )]
    (parser env' query)))

(defn follow-ref
  "Follow the given ref in the env, or just return the obj if it is not a ref."
  [{:keys [state]} obj-or-ref]
  (if (om/ref? obj-or-ref)
    (get-in @state obj-or-ref)
    obj-or-ref
    ))

(defn get-in-db-path [{:keys [db-path state] :as env}]
  (loop [node @state path db-path]
    (if (empty? path)
      node
      (let [k (first path)
            v (follow-ref env (get node k))]
        (recur v (rest path))
        ))))


(defn dbget
  "Get the specified key from the state in the environment, honoring the current :db-path in env. Follows refs to top-level tables."
  ([env key] (dbget env key nil))
  ([{:keys [state db-path] :as env} key dflt]
   (let [node-state (get-in-db-path env)
         value (get node-state key dflt)]
     (follow-ref env value)
     )))

(defn db-value
  "Get the specified key from the app state honoring the current :db-path. Follows refs."
  [env key]
  (if-let [v (dbget env key nil)]
    {:value v}
    nil
    ))

(defn parse-join-with-reader
  "
  When the current node in the database at :db-path contains a vector, use this function to walk the
  query from the env against each item in the database (which, for example, could recurse on each node).
  
  This function returns the (potentially recursively) retrieved data, or :missing if the given node does 
  not contain a vector. It will also follow refs (assumes refs can be resolved from root of db)
  
  Since your data might contain ref loops, the default recursion limit is 20. Use `:limit n` as 
  an additional named parameter to limit the depth of the recursion to `n`.
  
  Using the additional `:reset-depth n` can be used to re-root the depth at the current join. Be careful
  not to reset the join in a recursion such that it will go forever!
  "
  [reader {:keys [parser depth query] :or {depth 0} :as env} key & {:keys [limit reset-depth] :or {limit 20 reset-depth false}}]
  (if (>= depth limit)
    (do
      (println "Recursion limit (" limit ") reached.")
      nil)
    (let [items (dbget env key)
          to-many? (vector? items)
          to-one? (map? items)
          env' (cond-> env
                       to-many? (descend key)
                       to-one? (descend key)
                       :always (assoc :depth (or reset-depth (inc depth)) :reader reader)
                       :always (dissoc :query))
          ]
      (cond
        to-many? (into [] (map-indexed (fn [idx _] (parser (descend env' idx) query)) items))
        to-one? (parser env' query)
        :else :missing
        )
      )))

(defn elide-empty-query
  "Helper method to prevent a remote request if the sub-parser response is empty.
  
  `target`: Remote target name
  `response`: The response from a recursive call to the parser
  
  Emits {target response} if the response is non-empty; otherwise nil.
  "
  [target {:keys [query] :as response}]
  (if (or (nil? query) (empty? query))
    nil
    {target response})
  )

(defn recurse-remote
  "Recursively calls the parser on the subquery which will completely determine the result of
  whether or not a remote request is needed. Call this when you want to delegate the remote 
  response decision to the sub-query, and use the return value of this function as the proper
  return value of read. Returns {target query} if there should be a request, or nil)."
  [{:keys [target ast parser] :as env} key descend?]
  (let [env' (if descend? (descend env key) env)]
    (elide-empty-query target (update-in ast [:query] #(parser env' % target)))))

(defn fetch-if-missing
  "Terminate the descent of the parse and indicate the rest of the sub-query should run against the target indicated
  by the environment *if* the given key in the current app-state at :db-path is missing, nil, or `:missing`. 
  
  NOTE: If the *query* has a target on the AST (which indicates a forced read via a quote on the keyword) then this 
  function will always honor that and include the element in the remote fetch query.
   
  If the as-root? parameter is `true` or `:make-root`, then it indicates that the sub-query should be sent to the
  server as a root query, without any of the prior recursion's query prefix. If you use it, then you will also need
  to `process-roots` in your send method. Any other value can be used to indicate it is not a root.
  "
  [{:keys [target ast] :as env} key as-root?]
  (let [cached-read-ok? (not (= target (:target ast)))
        value (dbget env key nil)
        as-root? (or (true? as-root?) (= :make-root as-root?))]
    (if (and cached-read-ok? (not= nil value) (not= :missing value))
      nil
      {target
       (cond-> ast
               as-root? (assoc :query/root true)
               )}
      )))


