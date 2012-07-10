(ns graph-zip.core
  (:require [clojure.zip :as zip]
            [clojure.data.zip :as zip-filter]))

(defprotocol Graph
  (props-map [_ node direction])
  (prop-values [_ node prop direction]))

(extend-protocol Graph
  nil
  (props-map [_ _ _] nil)
  (prop-values [_ _ _ _] nil))

;; graph :: ^Graph
(defn graph-zipper [graph root-node]
  {:graph graph :node root-node})

(defn zipper-node [zipper]
  (:node zipper))

(defn zipper-graph [zipper]
  (:graph zipper))

(defn props
  ([zipper] (props zipper :out))
  ([zipper direction]
     (let [{:keys [node graph]} zipper]
       (or (props-map graph node direction)
           {}))))

(defn prop
  ([zipper prop-name] (prop zipper prop-name :out))
  ([zipper prop-name direction] 
     (let [{:keys [node graph]} zipper]
       (prop-values graph node prop-name direction))))

(defn prop-is
  ([prop-name pred] (prop-is prop-name pred :out))
  ([prop-name pred direction] (fn [loc]
                                (some #(pred %) (prop loc prop-name direction)))))

(defn prop=
  ([prop-name expected] (prop= prop-name expected :out))
  ([prop-name expected direction] (prop-is prop-name (partial = expected))))

(defn prop1
  ([zipper prop-name] (prop1 zipper prop-name :out))
  ([zipper prop-name direction]
      (let [result (prop zipper prop-name direction)]
        (if (= 1 (count result))
          (first result)
          nil))))

(defn go-to [node]
  (fn [zipper]
    (graph-zipper (zipper-graph zipper) node)))

(defn navigate-relationship [zipper rel direction]
  (let [{:keys [node graph]} zipper
        valid-child-nodes (prop-values graph node rel direction)]
    (for [node valid-child-nodes]
      (graph-zipper graph node))))

(defn incoming [prop-name]
  (fn [zipper]
    (navigate-relationship zipper prop-name :in)))

(defn zip->
  [zipper & preds]
  (zip-filter/mapcat-chain zipper preds
                           #(cond
                             (vector? %)
                             (fn [zipper] (and (seq (apply zip-> zipper %)) (list zipper)))
                             
                             (fn? %) nil
                             
                             :otherwise
                             (fn [zipper] (navigate-relationship zipper % :out)))))

(defn zip1->
  [zipper & preds]
  (let [result (apply zip-> zipper preds)]
    (if (= (count result) 1)
      (first result))))
