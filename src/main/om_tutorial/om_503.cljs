(ns om-tutorial.om-503
  (:require [om.next :as om]))

(defn rewrite [rewrite-map result]
  (letfn [(step [new-result [k orig-paths]]
            (let [result-to-move (get result k)
                  redistributed (reduce (fn [res path]
                                          (assoc-in res (conj path k) result-to-move)) new-result orig-paths)]
              (dissoc redistributed k)))]
    (reduce step result rewrite-map)))

(defn- alternate-roots
  "When given a join `{:join selector-vector}`, roots found so far, and a `path` prefix:
  returns a (possibly empty) sequence of [re-rooted-join prefix] results.
  Does NOT support sub-roots. Each re-rooted join will share only
  one common parent (their common branching point).
  "
  [join result-roots path]
  (letfn [(query-root? [join] (true? (-> join meta :query-root)))]
    (if (om/join? join)
      (if (query-root? join)
        (conj result-roots [join path])
        (mapcat #(alternate-roots % result-roots (conj path (om/join-key join))) (om/join-value join)))
      result-roots)))

(defn- merge-joins
  "Searches a query for duplicate joins and deep-merges them into a new query."
  [query]
  (letfn [(step [res query-element]
            (if (contains? (:elements-seen res) query-element)
              res                                           ; eliminate exact duplicates
              (update-in
                (if (and (om/join? query-element) (not (om/union? query-element)))
                  (let [k (om/join-key query-element)
                        v (om/join-value query-element)
                        q (or (-> res :query-by-join (get k)) [])
                        nq (if (or (= q '...) (= v '...))
                             '...
                             (merge-joins (into [] (concat q v))))]
                    (update-in res [:query-by-join] assoc k nq))
                  (update-in res [:non-joins] conj query-element))
                [:elements-seen] conj query-element)))]
    (let [{:keys [non-joins query-by-join]} (reduce step {:query-by-join {} :elements-seen #{} :non-joins []} query)
          merged-joins (mapv (fn [[jkey jsel]] {jkey jsel}) query-by-join)
          merged-query (into [] (concat non-joins merged-joins))]
      merged-query)))

(defn process-roots [query]
  "A send helper for rewriting the query to remove client local keys that
   don't need server side processing. Give a query this function will
   return a map with two keys, :query and :rewrite. :query is the
   actual query you should send. Upon receiving the response you should invoke
   :rewrite on the response before invoking the send callback."
  (let [retain (fn [ele] [[ele []]])                        ; emulate an alternate-root element
        reroots (mapcat (fn [qele]
                          (let [alt-roots (alternate-roots qele [] [])]
                            (if (empty? alt-roots)
                              (retain qele)
                              alt-roots))) query)
        query   (merge-joins (mapv first reroots))
        rewrite-map (reduce (fn [rewrites [ele path]]
                              (if (empty? path)
                                rewrites
                                (update-in rewrites [(om/join-key ele)] conj path))) {} reroots)]
    {:query   query
     :rewrite (partial rewrite rewrite-map)}))

