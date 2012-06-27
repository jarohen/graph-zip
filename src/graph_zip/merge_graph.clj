(ns graph-zip.merge-graph
  (:use [graph-zip.core]
        [graph-zip.in-memory :only [my-map build-in-memory-graph]]))

(defrecord MergeGraph [graphs]
  Graph
  (props-map [graphs node]
    (apply merge-with (comp flatten conj) (map #(props-map % node) (:graphs graphs))))
  (prop-values [graphs node prop]
    (flatten (map #(prop-values % node prop) (:graphs graphs)))))

(defn make-merge-graph [& graphs]
  (MergeGraph. graphs))

;; TESTS -----------------------

(def additional-map (build-in-memory-graph [{:subject "patbox" :property :instance :object "patbox/instance3"}
                                            {:subject "patbox" :property "cmdb:hostname" :object "DBLONWS33999"}
                                            {:subject "patbox/instance3" :property "label" :object "3"}]))

(def merged-loc (graph-zip (MergeGraph. [my-map additional-map]) "patbox"))

(loc-node (graph1-> merged-loc
                        :instance
                        (prop= "label" "3"))) ;; -> "patbox/instance3"

(loc-node (graph1-> merged-loc
                        :instance
                        (prop= "label" "1"))) ;; -> "patbox/instance"

(map loc-node (graph-> merged-loc
                       :instance)) ;; -> ("patbox/instance3" "patbox/instance2" "patbox/instance")


