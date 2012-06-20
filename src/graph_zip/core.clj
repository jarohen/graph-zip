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
  (map #(vector (second %) graph) (val (find graph node))))

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

(defn prop [loc prop]
  (let [[node graph] (zip/node loc)]
    (map second (filter #(= prop (first %)) (val (find graph node))))))

(defn prop1 [loc prop-name]
  (let [result (prop loc prop-name)]
    (if (= (count result) 1)
      (first result))))

(defn prop=
  [prop expected]
  (fn [loc]
    (let [[node graph] (zip/node loc)]
      (some #(= [prop expected] %) (val (find graph node))))))

(defn- child-nodes-by-pred [node graph pred]
  (map second (filter #(= (first %) pred) (val (find graph node)))))

(defn- loc-to-node [loc]
  (first (zip/node loc)))

(defn- navigate-relationship [loc pred]
  (let [[node graph] (zip/node loc)
        valid-child-nodes (child-nodes-by-pred node graph pred)
        child-locs (zf/children loc)
        valid-child-locs (filter (fn [child-loc]
                                   (let [child-node (loc-to-node child-loc)]
                                     (some #(= child-node %) valid-child-nodes)))
                                 child-locs)]
    valid-child-locs))

(defn graph-node [loc] (first (zip/node loc)))

(defn graph->
  [loc & preds]
  (zf/mapcat-chain loc preds
                   #(cond
                     (vector? %) (fn [loc] (and (seq (apply graph-> loc %)) (list loc)))
                     (or (keyword? %) (string? %)) (fn [loc] (navigate-relationship loc %)))))


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
  (map graph-node (graph-> loc :instance [(prop= "label" "1")] "cmdb:jvm" "cmdb:maxMem")))

(let [loc (-> (graph-zipper (build-graph-map [{:subject "patbox" :property :instance :object "patbox/instance"}
                                                  {:subject "patbox" :property :instance :object "patbox/instance2"}
                                                  {:subject "patbox/instance" :property :userid :object "mis"}
                                                  {:subject "patbox/instance" :property "label" :object "1"}
                                                  {:subject "patbox/instance2" :property "label" :object "2"}
                                                  {:subject "patbox/instance" :property "cmdb:jvm" :object "patbox/instance/jvm"}
                                                   {:subject "patbox/instance/jvm" :property "cmdb:maxMem" :object "1024m"}])
                            "patbox"))]
  (map graph-node (graph-> loc :instance (prop= "label" "2"))))

(let [loc (graph-zipper (build-graph-map [{:subject "patbox" :property :instance :object "patbox/instance"}
                                          {:subject "patbox" :property :instance :object "patbox/instance2"}
                                          {:subject "patbox/instance" :property :userid :object "mis"}
                                          {:subject "patbox/instance" :property "label" :object "1"}
                                          {:subject "patbox/instance2" :property "label" :object "2"}
                                          {:subject "patbox/instance" :property "cmdb:jvm" :object "patbox/instance/jvm"}
                                          {:subject "patbox/instance/jvm" :property "cmdb:maxMem" :object "1024m"}])
                        "patbox")]
  (map graph-node (graph-> loc :instance)))