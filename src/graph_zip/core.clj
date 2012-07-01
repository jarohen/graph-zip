(ns graph-zip.core
  (:require [clojure.zip :as zip]
            [clojure.data.zip :as zip-filter]))

(defprotocol Graph
  (props-map [_ node])
  (prop-values [_ node prop]))

(extend-protocol Graph
  nil
  (props-map [_ _] nil)
  (prop-values [_ _ _] nil))

(defn- graph-children [{:keys [node graph]}]
  (map #(hash-map :node % :graph graph) (mapcat val (props-map graph node))))

(defn- graph-make-node [_ _ _]
  ;; TODO It might be possible
  
  ;; We can't modify a graph using zipper because zipper makes the assumption
  ;; that the parents of a node can't change when you modify a child node. Graphs
  ;; can be cyclical (unlike vec, seq, or xml), which means it is possible to edit
  ;; higher up a traversal, and so the changes wouldn't be reflected if the user
  ;; traversed back up the tree.
  (throw (RuntimeException. "Can't modify graph using zipper.")))

;; graph :: ^Graph
(defn graph-zipper [graph root-node]
  (zip/zipper
   (constantly true) ;;graph-branch?
   graph-children
   graph-make-node
   {:node root-node :graph graph}))

(defn zipper-node [zipper]
  (if-let [node (zip/node zipper)]
    (:node node)))

(defn zipper-graph [zipper]
  (if-let [node (zip/node zipper)]
    (:graph node)))

(defn props [zipper]
  (let [{:keys [node graph]} (zip/node zipper)]
    (or (props-map graph node)
        {})))

(defn prop [zipper prop-name]
  (let [{:keys [node graph]} (zip/node zipper)]
    (prop-values graph node prop-name)))

(defn prop=
  [prop-name expected]
  (fn [zipper]
    (let [{:keys [graph node]} (zip/node zipper)]
      (some #(= expected %) (prop-values graph node prop-name)))))

(defn prop1 [zipper prop-name]
  (let [result (prop zipper prop-name)]
    (if (= 1 (count result))
      (first result)
      nil)))

(defn go-to [node]
  (fn [zipper]
    (vector (graph-zipper (zipper-graph zipper) node))))

(defn navigate-relationship [zipper rel]
  (let [{:keys [node graph]} (zip/node zipper)
        valid-child-nodes (prop-values graph node rel)
        child-zippers (zip-filter/children zipper)
        valid-child-zippers (filter (fn [child-zipper]
                                   (let [child-node (zipper-node child-zipper)]
                                     (some #(= child-node %) valid-child-nodes)))
                                 child-zippers)]
    valid-child-zippers))

(defn zip->
  [zipper & preds]
  (zip-filter/mapcat-chain zipper preds
                           #(cond
                             (vector? %)
                             (fn [zipper] (and (seq (apply zip-> zipper %)) (list zipper)))
                             
                             (fn? %) nil
                             
                             :otherwise
                             (fn [zipper] (navigate-relationship zipper %)))))

(defn zip1->
  [zipper & preds]
  (let [result (apply zip-> zipper preds)]
    (if (= (count result) 1)
      (first result))))
