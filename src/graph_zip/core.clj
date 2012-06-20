(ns graph-zip.core
  (:use [clojure.data.zip.xml :only [xml->]])
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.data.zip :as zf]))

(defn- add-statement-to-map [graph-map {:keys [subject property object]}]
  (assoc graph-map subject
         (if-let [existing-props (get graph-map subject)]
           (assoc existing-props property
                  (if-let [existing-vals (get existing-props property)]
                    (cons object existing-vals)
                    [object]))
           {property [object]})))

(defn- graph-branch? [_] true)

(defn- graph-children [{:keys [object graph]}]
  (map #(hash-map :object % :graph graph) (mapcat val (get graph object))))

(defn- graph-make-node [_ _ _]
  ;; TODO It probably is possible, I'm just a clojure newbie...
  
  ;; We can't modify a graph using zipper because zipper makes the assumption
  ;; that the parents of a node can't change when you modify a child node. Graphs
  ;; can be cyclical (unlike vec, seq, or xml), which means it is possible to edit
  ;; higher up a traversal, and so the changes wouldn't be reflected if the user
  ;; traversed back up the tree.
  (throw (RuntimeException. "Can't modify graph using zipper.")))

;; graph-map :: {subject -> [property object]}
(defn graph-zipper [graph-map root-object]
  (zip/zipper
   graph-branch?
   graph-children
   graph-make-node
   {:object root-object :graph graph-map}))

;; statements :: [{:subject :property :object}]
(defn build-graph-map [statements]
  (reduce add-statement-to-map
      (hash-map)
      statements))

(defn prop [loc prop]
  (let [{:keys [object graph]} (zip/node loc)
        prop-map (get graph object)]
    (get prop-map prop)))

(defn prop1 [loc prop-name]
  (let [result (prop loc prop-name)]
    (if (= (count result) 1)
      (first result))))

(defn prop=
  [prop-name expected]
  (fn [loc]
    (some #(= expected %) (prop loc prop-name))))

(defn- child-objects-by-pred [object graph pred]
  (if-let [preds (get graph object)]
    (get preds pred)))

(defn graph-object [loc] (:object (zip/node loc)))

(defn- navigate-relationship [loc pred]
  (let [{:keys [object graph]} (zip/node loc)
        valid-child-objects (child-objects-by-pred object graph pred)
        child-locs (zf/children loc)
        valid-child-locs (filter (fn [child-loc]
                                   (let [child-object (graph-object child-loc)]
                                     (some #(= child-object %) valid-child-objects)))
                                 child-locs)]
    valid-child-locs))

(defn graph->
  [loc & preds]
  (zf/mapcat-chain loc preds
                   #(cond
                     (vector? %) (fn [loc] (and (seq (apply graph-> loc %)) (list loc)))
                     (or (keyword? %) (string? %)) (fn [loc] (navigate-relationship loc %)))))

(defn graph1->
  [loc & preds]
  (let [result (apply graph-> loc preds)]
    (if (= (count result) 1)
      (first result))))

;; ----------- TESTS


(def my-map (build-graph-map [{:subject "patbox" :property :instance :object "patbox/instance"}
                              {:subject "patbox" :property :instance :object "patbox/instance2"}
                              {:subject "patbox/instance" :property :userid :object "mis"}
                              {:subject "patbox/instance" :property "label" :object "1"}
                              {:subject "patbox/instance2" :property "label" :object "2"}
                              {:subject "patbox/instance" :property "cmdb:jvm" :object "patbox/instance/jvm"}
                              {:subject "patbox/instance/jvm" :property "cmdb:maxMem" :object "1024m"}]))

(def patbox-loc (graph-zipper my-map "patbox"))

(graph-object (graph1-> patbox-loc
                        :instance
                        [(prop= "label" "1")]
                        "cmdb:jvm"
                        "cmdb:maxMem")) ;; -> "1024m"

(graph-object (graph1-> patbox-loc
                        :instance
                        (prop= "label" "2"))) ;; -> "patbox/instance2"

(map graph-object (graph-> patbox-loc
                           :instance)) ;; -> ("patbox/instance2" "patbox/instance")
