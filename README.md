You can use graph zip like this

    ;; Dependencies for an in-memory graph
    (use 'graph-zip.core :only [graph-zip zip-> zip1-> zipper-node prop=])
    (use 'graph-zip.in-memory :only [build-in-memory-graph])


    ;; Build up the in-memory graph - a list of triples, each with a :subject, :property and :object
    (def my-map (build-in-memory-graph [{:subject "prod-host" :property :instance :object "prod-host/instance"}
                                        {:subject "prod-host" :property :instance :object "prod-host/instance2"}
                                        {:subject "prod-host/instance" :property :userid :object "my-user"}
                                        {:subject "prod-host/instance" :property "label" :object "1"}
                                        {:subject "prod-host/instance2" :property "label" :object "2"}
                                        {:subject "prod-host/instance" :property "cmdb:jvm" :object "prod-host/instance/jvm"}
                                        {:subject "prod-host/instance/jvm" :property "cmdb:maxMem" :object "1024m"}]))

    ;; Build a re-usable zipper, rooted at the "prod-host" node
    (def prod-host-zipper (graph-zipper my-map "prod-host"))

    ;; This 'zips' along the :instance relation, and returns a list of zippers.
    ;; Mapping zipper-node over the zippers returns just the nodes that are related to "prod-host" by :instance
    (map zipper-node (zip-> prod-host-zipper
                            :instance)) ;; -> ("prod-host/instance2" "prod-host/instance")

    ;; In a similar vein to xml-zip, you can prune the graph by using a 'prop=' predicate
    ;; If you are only expecting one result, you can use the zip1-> function instead.
    (zipper-node (zip1-> prod-host-zipper
                         :instance
                         (prop= "label" "2"))) ;; -> "prod-host/instance2"

    ;; Relations don't have to be keywords
    (zipper-node (zip1-> prod-host-zipper
                         :instance
                         [(prop= "label" "1")]
                         "cmdb:jvm"
                         "cmdb:maxMem")) ;; -> "1024m"

    

    


