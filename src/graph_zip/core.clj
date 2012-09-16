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
(defn graph-zip [graph root-node]
  {:graph graph :node root-node})

(defn node [loc]
  (:node loc))

(defn graph [loc]
  (:graph loc))

(defn props [loc]
  (let [{:keys [node graph]} loc]
    (or (props-map graph node :out) {})))

(defn prop-is [prop-name pred]
  (fn [loc]
    (when (some #(pred %)
                (let [{:keys [node graph]} loc]
                  (prop-values graph node prop-name :out)))
      loc)))

(defn prop= [prop-name expected]
  (prop-is prop-name (partial = expected)))

(defn go-to [node]
  (fn [loc]
    (graph-zip (graph loc) node)))

(defn- navigate-relationship [loc rel direction]
  (let [{:keys [node graph]} loc
        valid-child-nodes (prop-values graph node rel direction)]
    (for [node valid-child-nodes]
      (graph-zip graph node))))

(defn incoming [prop-name]
  (fn [loc]
    (navigate-relationship loc prop-name :in)))

(defn zip->
  [loc & preds]
  (when loc
    (zip-filter/mapcat-chain loc preds
                             #(cond
                               (vector? %)
                               (fn [loc] (and (seq (apply zip-> loc %)) (list loc)))
                                       
                               (fn? %) nil
                                       
                               :otherwise
                               (fn [loc] (navigate-relationship loc % :out))))))

(defn zip1->
  [loc & preds]
  (let [result (apply zip-> loc preds)]
    (when (= (count result) 1)
      (first result))))
