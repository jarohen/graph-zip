(ns graph-zip.merge-graph
  (:use [graph-zip.core]
        [graph-zip.in-memory :only [my-map build-in-memory-graph]]))

(defrecord MergeGraph [graphs]
  Graph
  (props-map [this node]
    (let [maps (map #(props-map % node) (:graphs this))
          non-empty-maps (filter #(not (empty? %)) maps)]
      (apply merge-with (comp flatten conj) non-empty-maps)))
  (prop-values [this node prop]
    (flatten (filter #(not (empty? %)) (map #(prop-values % node prop) (:graphs this))))))

(defn make-merge-graph [& graphs]
  (MergeGraph. graphs))

;; TESTS -----------------------

(def additional-map (build-in-memory-graph [{:subject "patbox" :property :instance :object "patbox/instance3"}
                                            {:subject "patbox" :property "cmdb:hostname" :object "DBLONWS33999"}
                                            {:subject "patbox/instance3" :property "label" :object "3"}]))

(def merged-loc (graph-zipper (MergeGraph. [my-map additional-map]) "patbox"))

(loc-node (zip1-> merged-loc
                  :instance
                  (prop= "label" "3"))) ;; -> "patbox/instance3"

(loc-node (zip1-> merged-loc
                  :instance
                  (prop= "label" "1"))) ;; -> "patbox/instance"

(map loc-node (zip-> merged-loc
                     :instance)) ;; -> ("patbox/instance3" "patbox/instance2" "patbox/instance")


