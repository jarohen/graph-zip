(ns graph-zip.core
  (:use [clojure.data.zip.xml :only [xml->]])
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.data.zip :as zf]))

(defn- add-statement-to-map [graph-map {:keys [subject property object]}]
  (assoc graph-map subject
          (if-let [existing-props (get graph-map subject)]
            (cons [property object] existing-props)
            [[property object]])))

(defn- graph-branch? [_] true)

(defn- graph-children [[node graph]]
  (map second (val (find graph node))))

(defn- graph-make-node [_ _ _]
  ;; We can't modify a graph using zipper because zipper makes the assumption
  ;; that the parents of a node can't change when you modify a child node. Graphs
  ;; can be cyclical (unlike vec, seq, or xml), which means it is possible to edit
  ;; higher up a traversal, and so the changes wouldn't be reflected if the user
  ;; traversed back up the tree.
  (throw (RuntimeException. "Can't modify graph using zipper.")))

;; graph-map :: {subject -> [property object]}
(defn graph-zipper [graph-map root-subject]
  (zip/zipper
   graph-branch?
   graph-children
   graph-make-node
   [root-subject graph-map]))

;; statements :: [{:subject :property :object}]
(defn build-graph-map [statements]
  (reduce add-statement-to-map
      (hash-map)
      statements))

(defn prop=
  [prop expected]
  (fn [[node graph]]
    (some #(and
            (= (first %) prop)
            (= (second %) expected))
          (val (find graph node)))))

(defn graph->
  [loc & preds]
  (println (format "Applying graph->%n loc = %s%n preds=%s" loc preds))
  (zf/mapcat-chain loc preds
                   #(cond
                     (vector? %) (fn [loc] (and (seq (apply graph-> loc %)) (list loc)))
                     (keyword? %) (fn [loc] (let [[node graph] (zip/node loc)]
                                              (map (fn [n] [(second n) graph])
                                                   (filter (fn [c] (= (first c) %))
                                                           (val (find graph node)))))))))

;; ----------- TESTS


(def my-map (build-graph-map [{:subject "patbox" :property :instance :object "patbox/instance"}
                              {:subject "patbox" :property :instance :object "patbox/instance2"}
                              {:subject "patbox/instance" :property :userid :object "mis"}
                              {:subject "patbox/instance" :property "label" :object "1"}
                              {:subject "patbox/instance2" :property "label" :object "2"}
                              {:subject "patbox/instance" :property "cmdb:jvm" :object "patbox/instance/jvm"}
                              {:subject "patbox/instance/jvm" :property "cmdb:maxMem" :object "1024m"}]))


(let [loc (-> (graph-zipper (build-graph-map [{:subject "patbox" :property :instance :object "patbox/instance"}
                                                  {:subject "patbox" :property :instance :object "patbox/instance2"}
                                                  {:subject "patbox/instance" :property :userid :object "mis"}
                                                  {:subject "patbox/instance" :property "label" :object "1"}
                                                  {:subject "patbox/instance2" :property "label" :object "2"}
                                                  {:subject "patbox/instance" :property "cmdb:jvm" :object "patbox/instance/jvm"}
                                                   {:subject "patbox/instance/jvm" :property "cmdb:maxMem" :object "1024m"}])
                            "patbox"))]
  (graph-> loc :instance [(prop= "label" "1")])) ;; -> "1024m"

(let [loc (graph-zipper (build-graph-map [{:subject "patbox" :property :instance :object "patbox/instance"}
                                          {:subject "patbox" :property :instance :object "patbox/instance2"}
                                          {:subject "patbox/instance" :property :userid :object "mis"}
                                          {:subject "patbox/instance" :property "label" :object "1"}
                                          {:subject "patbox/instance2" :property "label" :object "2"}
                                          {:subject "patbox/instance" :property "cmdb:jvm" :object "patbox/instance/jvm"}
                                          {:subject "patbox/instance/jvm" :property "cmdb:maxMem" :object "1024m"}])
                        "patbox")]
  (graph-> loc :instance))

((prop= "label" "2") ["patbox/instance2" my-map])
