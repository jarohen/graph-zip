(ns graph-zip.dot
  (:use
   graph-zip.core
   graph-zip.in-memory))

(letfn [(->dot [graph]
          (let [out-graph (:out (:graphs graph))])
          (format "digraph {%n%s%n}"
                  (apply str (interpose ";\n" (apply concat
                                                     (for [[table-uri fks] dot-data]
                                                       (conj (for [[col-uri fk-table-uri] fks]
                                                               (format "\"%s\" -> \"%s\""
                                                                       (:uri table-uri)
                                                                       (:uri fk-table-uri)))
                                                             (format "\"%s\"" (:uri table-uri)))))))))]
  (->dot my-map))