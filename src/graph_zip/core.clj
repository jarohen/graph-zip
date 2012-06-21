(ns graph-zip.core
  (:use [clojure.data.zip.xml :only [xml->]])
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.data.zip :as zf]))

(defprotocol Graph
  (props-map [_ node])
  (prop-values [_ node prop]))

(extend-protocol Graph
  nil
  (props-map [_ _] nil)
  (prop-values [_ _ _] nil))

(defn- graph-branch? [_] true)

(defn- graph-children [{:keys [node graph]}]
  (map #(hash-map :node % :graph graph) (mapcat val (props-map graph node))))

(defn- graph-make-node [_ _ _]
  ;; TODO It probably is possible, I'm just a clojure newbie...
  
  ;; We can't modify a graph using zipper because zipper makes the assumption
  ;; that the parents of a node can't change when you modify a child node. Graphs
  ;; can be cyclical (unlike vec, seq, or xml), which means it is possible to edit
  ;; higher up a traversal, and so the changes wouldn't be reflected if the user
  ;; traversed back up the tree.
  (throw (RuntimeException. "Can't modify graph using zipper.")))

;; graph-map :: ^Graph
(defn graph-zipper [graph root-node]
  (zip/zipper
   graph-branch?
   graph-children
   graph-make-node
   {:node root-node :graph graph}))

(defn props [loc]
  (let [{:keys [node graph]} (zip/node loc)]
    (props-map graph node)))

(defn prop [loc prop-name]
  (let [{:keys [node graph]} (zip/node loc)]
    (prop-values graph node prop-name)))



(defn prop=
  [prop-name expected]
  (fn [loc]
    (let [{:keys [graph node]} (zip/node loc)]
      (some #(= expected %) (prop-values graph node prop-name)))))

(defn prop1 [loc prop-name]
  (let [result (prop loc prop-name)]
    (if (= 1 (count result))
      (first result)
      nil)))

(defn loc-node [loc]
  (if (nil? loc)
    nil
    (:node (zip/node loc))))

(defn loc-graph [loc]
  (if (nil? loc)
    nil
    (:graph (zip/node loc))))

(defn navigate-relationship [loc rel]
  (let [{:keys [node graph]} (zip/node loc)
        valid-child-nodes (prop-values graph node rel)
        child-locs (zf/children loc)
        valid-child-locs (filter (fn [child-loc]
                                   (let [child-node (loc-node child-loc)]
                                     (some #(= child-node %) valid-child-nodes)))
                                 child-locs)]
    valid-child-locs))

(defn graph->
  [loc & preds]
  (zf/mapcat-chain loc preds
                   #(cond
                     (vector? %)
                     (fn [loc] (and (seq (apply graph-> loc %)) (list loc)))

                     (fn? %) nil
                     
                     :otherwise
                     (fn [loc] (navigate-relationship loc %)))))

(defn graph1->
  [loc & preds]
  (let [result (apply graph-> loc preds)]
    (if (= (count result) 1)
      (first result))))