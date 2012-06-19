(ns graph-zip.core
  (:require [clojure.zip :as zip]))

(defn- add-statement-to-map [graph-map {:keys [subject property object]}]
  (assoc graph-map subject
          (if-let [existing-props (get graph-map subject)]
            (cons [property object] existing-props)
            [[property object]])))

(defn- graph-branch? [_] true)

(defn- graph-children? [graph-map]
  (fn [[_ uri]]
    (if-let [entry (find graph-map uri)]
      (val entry)
      [])))

(defn- graph-make-node [_ _ _]
  ;; We can't modify a graph using zipper because zipper makes the assumption
  ;; that the parents of a node can't change when you modify a child node. Graphs
  ;; can be cyclical (unlike vec, seq, or xml), which means it is possible to edit
  ;; higher up a traversal, and so the changes wouldn't be reflected if the user
  ;; traversed back up the tree.
  throw (RuntimeException. "Can't modify graph using zipper."))

;; graph-map :: {subject -> [property object]}
(defn graph-zipper [graph-map root-subject]
  (zip/zipper
   graph-branch?
   (graph-children? graph-map)
   graph-make-node
   [nil root-subject]))

;; statements :: [{:subject :property :object}]
(defn build-graph-map [statements]
  (reduce add-statement-to-map
      (hash-map)
      statements))

  
