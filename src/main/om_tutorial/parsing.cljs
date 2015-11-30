(ns om-tutorial.parsing
  (:require [cljs.pprint :refer [pprint]] [om.next :as om]))

(defn dbg [msg v] (println msg v) v)

(defn new-read-entry-point
  "Create a new read entry point that allows you to switch read functions during a local parse, and can
  be configured with an alternate reader for each remote.

  The remote-read-map should be a map keyed by remote name, whose values are read functions
  (e.g. `{:remote (fn [e k p] ...)}`)
  "
  [default-local-read remote-read-map]
  (fn [{:keys [target ast reader] :as env} key params]
    (let [env' (if (contains? env :db-path) (update-in env [:query-path] conj (:key ast))
                                            (assoc env :query-path [key] :db-path []))]
      (cond
        reader (reader env' key params)
        (contains? remote-read-map target) ((get remote-read-map target) env' key params)
        :else (default-local-read env' key params)
        ))))

;;********************************************************************************
;; The rest of this namespace is meant to be of abstract use. I've tried to make the code as
;; clear as possible, and have written some tests in the devcards (which would allow you to
;; play with them more as well).

(defn descend
  "Recursively descend the state database, tracking position in :db-path. If given a ref as the key, rewrites
   db-path to be rooted at that ref.
  "
  [env key]
  (if (om/ident? key)
    (assoc env :db-path [key])
    (update-in env [:db-path] conj key)))

(defn parse-with-reader
  "Cause this parse to recursively descend, but switch to using the named reader function.

  DEPRECATED? I think the join processing will always be used over this... This was a first attempt function

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
  (if (om/ident? obj-or-ref)
    (get-in @state obj-or-ref)
    obj-or-ref
    ))

(defn get-in-db-path
  "This is just like get-in; however, it walks the path and if it hits an Om ref, it will
  follow that to the real object. This is useful for walking the overall graph as the query
  might need."
  [{:keys [db-path state] :as env}]
  (loop [node @state path db-path]
    (if (empty? path)
      node
      (let [k (first path)
            v (if (om/ident? k) (get-in @state k) (follow-ref env (get node k)))]
        (recur v (rest path))
        ))))


(defn dbget
  "Get the specified key from the state in the environment, honoring the current :db-path.
  Follows refs (in the db-path variable OR the app state) to top-level tables."
  ([env key] (dbget env key nil))
  ([{:keys [state db-path] :as env} key dflt]
   (if (om/ident? key)
     (get-in @state key dflt)
     (let [node-state (get-in-db-path env)
           value (get node-state key dflt)]
       (follow-ref env value)
       ))))

(defn db-value
  "Exactly equivalent to {:value (dbget env key nil)}. Useful as immediate return value of read."
  [env key]
  (if-let [v (dbget env key nil)]
    (do
      {:value v})
    nil
    ))

(defn parse-join-with-reader
  "
  This function is meant for walking joins in the application state. It will track the position via
  the key `:db-path` in the parse env. It can handle to-many and to-one joins by detecting the
  data that actually exists in the state.

  When the current node in the state at :db-path is a vector, this function will walk the
  sub-query against each item in the state (which, for example, could recurse further on each node
  if there are sub-joins).

  When the node is an object it applies the sub-query against that object (possibly recursing futher if
  it finds more joins).

  All Om refs are automatically resolved (followed). It is possible this can create infinite loops (especially if
  you use the `...` notation in a join query). To prevent this, a `:depth` key is added to the env to track
  the current depth of joins (from root), and recursion will abort (peacefully) at the default recursion limit of 20.
  Use `:limit n` as an additional named parameter to limit the depth of the recursion to `n`.

  Using the additional `:reset-depth n` can be used to re-root the depth at the current join (instead of counting from root).
  Be careful not to reset the join in a recursion such that it will go forever!

  The first argument to this function can also be used to switch which function will be called to handle the reading
  of the object(s) found at the join. Specifying `nil` reverts to the top-level (default) read function.
  "
  [reader {:keys [parser depth query] :or {depth 0} :as env} key & {:keys [limit reset-depth] :or {limit 20 reset-depth false}}]
  (if (>= depth limit)
    nil
    (let [items (dbget env key)
          to-many? (vector? items)
          to-one? (map? items)
          env' (-> env
                   (descend key)
                   (assoc :depth (or reset-depth (inc depth)) :reader reader)
                   )
          ]
      (cond
        to-many? (into [] (map-indexed (fn [idx _] (parser (descend env' idx) query)) items))
        to-one? (parser env' query)
        :else nil
        )
      )))

(defn ref-at-db-path
  "Returns the ref at the :db-path in the environment instead of following it to an object. Returns nil if the
  item at the db-path is not an Ident."
  [{:keys [db-path state] :as env}]
  (loop [node @state path db-path]
    (let [k (first path)
          v (if (om/ident? k) (get-in @state k) (get node k))
          v' (follow-ref env v)]
      (if (= 1 (count path))
        (cond
          (om/ident? k) k
          (om/ident? v) v
          :else nil)
        (recur v' (rest path))
        ))))

(defn ui-key
  "Transform a component Om ref into the proper UI property top-level key."
  [ref]
  (let [[persistent-key id] ref]
    [(keyword (str "ui." (namespace persistent-key)) (name persistent-key)) id]
    ))

(defn ui-attribute
  "Read a UI attribute that is pretending to be on the object at the current :db-path in the parse env. This
  function should be used in conjunction with attributes namespaced as `:ui/...` so that the other read helpers
  can remove them from remote queries (TODO)."
  [{:keys [state db-path] :as env} key]
  (if-let [ref (ref-at-db-path env)]
    (let [uikey (ui-key ref)
          node (get-in @state uikey)
          value (get node key)]
      (when value {:value value})
      )))

(defn elide-empty-query
  "Helper method to prevent a remote request parse for the current key if the sub-parser response is empty.

  `target`: Remote target name
  `response`: The response from a recursive call to the parser

  Emits {target response} if the response is non-empty; otherwise nil. Used by `recurse-remote`.
  "
  [target {:keys [query] :as response}]
  (if (or (nil? query) (empty? query))
    nil
    {target response})
  )

(defn- is-ui-query-fragment? [kw]
  (when (keyword? kw)
    (some->> kw namespace (re-find #"^ui(?:\.|$)"))))
(defn- remove-ui-query-fragments [v]
  (->> v
       (remove is-ui-query-fragment?)
       (remove #(when (list? %)
                  (-> % first is-ui-query-fragment?)))
       vec))
(defn strip-ui [query]
  (clojure.walk/prewalk #(if (vector? %)
                           (remove-ui-query-fragments %) %)
                        query))

(defn recurse-remote
  "Recursively calls the parser on the subquery (which will completely determine the result of
  whether or not a remote request is needed). Call this when you want to delegate the remote
  response decision to the sub-query, and use the return value of this function as the proper
  return value of read. Returns {target query} if there should be a request, or nil).

  Basically, use this on any client-local keyword that needs to be skipped over to get to the
  'real' server query.
  "
  [{:keys [target ast parser] :as env} key descend?]
  (let [env' (if descend? (descend env key) env)]
    (if (:target ast) "FORCED REMOTE READ")
    (elide-empty-query target (update-in ast [:query] #(let [v (parser env' % target)]
                                                        v)))))

(defn fetch-if-missing
  "Terminate the descent of the parse and indicate the rest of the sub-query should run against the target indicated
  by the environment *if* the given key in the current app-state at :db-path is missing or nil.

  NOTE: If the *query* has a target on the AST (which indicates a forced read via a quote on the keyword) then this
  function will always honor that and include the element in the remote fetch query.

  If the as-root? parameter is `true` or `:make-root`, then it indicates that the sub-query should be sent to the
  server as a root query, without any of the prior recursion's query prefix. If you use it, then you will also need
  to `process-roots` in your send method. Any other value can be used to indicate it is not a root.

  TODO: Remove (recursively) attributes in the `ui` namespace.
  "
  [{:keys [target ast] :as env} key as-root?]
  (let [cached-read-ok? (not (= target (:target ast)))
        value (dbget env key nil)
        as-root? (or (true? as-root?) (= :make-root as-root?))]
    (if (and cached-read-ok? (not= nil value))
      nil
      {target
       (cond-> ast
               as-root? (assoc :query-root true)
               )}
      )))


