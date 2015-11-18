(ns om-tutorial.nested-query
  (:require [cljs.pprint :refer [pprint]]
            [om.next :as om]))

(def app-state (atom {
                      :window/size  [1920 1200]
                      :friends      #{1 3} ; these are people IDs...see map below for the objects themselves   
                      :people/by-id {
                                     1 {:id 1 :name "Sally" :age 22 :married false}
                                     2 {:id 2 :name "Joe" :age 22 :married false}
                                     3 {:id 3 :name "Paul" :age 22 :married true :married-to 2}
                                     4 {:id 4 :name "Mary" :age 22 :married false}}
                      }))

(def query-props [:window/size {:friends [:name :married :married-to]}])
(def query-joined [:window/size {:friends [:name :married {:married-to [:name]}]}])

(defmulti rread om/dispatch)

(defmethod rread :default [{:keys [state]} key params] (println "YOU MISSED " key) nil)

(defmethod rread :window/size [{:keys [state]} key params] {:value (get @state :window/size)})

(defmethod rread :name [{:keys [person query]} key params] {:value (get person key)})
(defmethod rread :age [{:keys [person query]} key params] {:value (get person key)})
(defmethod rread :married [{:keys [person query]} key params] {:value (get person key)})

(defmethod rread :married-to
  ;; person is placed in env by rread :friends                                                                 
  [{:keys [state person parser query] :as env} key params]
  (let [partner-id (:married-to person)]
    (cond
      (and query partner-id) {:value [(select-keys (get-in @state [:people/by-id partner-id]) query)]}
      :else {:value partner-id}
      )))

(defmethod rread :friends [{:keys [state query parser path] :as env} key params]
  (let [friend-ids (get @state :friends)
        keywords (filter keyword? query)
        joins (filter map? query)
        get-person (fn [id]
                     (let [raw-person (get-in @state [:people/by-id id])
                           env' (dissoc env :query)
                           env-with-person (assoc env' :person raw-person)]
                       ;; recursively call parser w/modified env                                                
                       (parser env-with-person query)
                       ))
        friends (mapv get-person friend-ids)]
    {:value friends}
    )
  )

(def my-parser (om/parser {:read rread}))

(comment
  ;; remember to add a require for cljs.pprint to your namespace                                                 
  (my-parser {:state app-state} query-props)
  (my-parser {:state app-state} query-joined))  
