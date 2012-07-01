(ns graph-zip.merge-graph
  (:use [graph-zip.core]
        [graph-zip.in-memory :only [my-map build-in-memory-graph]]))

(defrecord MergeGraph [graphs]
  Graph
  (props-map [this node direction]
    (let [maps (map #(props-map % node direction) (:graphs this))
          non-empty-maps (filter #(not (empty? %)) maps)]
      (apply merge-with (comp flatten conj) non-empty-maps)))
  (prop-values [this node prop direction]
    (flatten (filter #(not (empty? %)) (map #(prop-values % node prop direction) (:graphs this))))))

(defn make-merge-graph [& graphs]
  (MergeGraph. graphs))

;; TESTS -----------------------

(def additional-map (build-in-memory-graph [{:subject "prod-host" :property :instance :object "prod-host/instance3"}
                                            {:subject "prod-host" :property "cmdb:hostname" :object "prod-server.example.com"}
                                            {:subject "prod-host/instance3" :property "label" :object "3"}]))

(def merged-zipper (graph-zipper (MergeGraph. [my-map additional-map]) "prod-host"))

(zipper-node (zip1-> merged-zipper
                     :instance
                     (prop= "label" "3"))) ;; -> "prod-host/instance3"

(zipper-node (zip1-> merged-zipper
                     :instance
                     (prop= "label" "1"))) ;; -> "prod-host/instance"

(map zipper-node (zip-> merged-zipper
                        :instance)) ;; -> ("prod-host/instance3" "prod-host/instance2" "prod-host/instance")


