You can use graph zip like this

    (use 'graph-zip.core :only [graph-zip])
    (use 'graph-zip.in-memory :only [build-in-memory-graph])

    (def my-map (build-in-memory-graph [{:subject "prod-host" :property :instance :object "prod-host/instance"}
                                        {:subject "prod-host" :property :instance :object "prod-host/instance2"}
                                        {:subject "prod-host/instance" :property :userid :object "my-user"}
                                        {:subject "prod-host/instance" :property "label" :object "1"}
                                        {:subject "prod-host/instance2" :property "label" :object "2"}
                                        {:subject "prod-host/instance" :property "cmdb:jvm" :object "prod-host/instance/jvm"}
                                        {:subject "prod-host/instance/jvm" :property "cmdb:maxMem" :object "1024m"}]))

    (def prod-host-zipper (graph-zipper my-map "prod-host"))

    (zipper-node (zip1-> prod-host-zipper
                         :instance
                         [(prop= "label" "1")]
                         "cmdb:jvm"
                         "cmdb:maxMem")) ;; -> "1024m"

    (zipper-node (zip1-> prod-host-zipper
                         :instance
                         (prop= "label" "2"))) ;; -> "prod-host/instance2"

    (map zipper-node (zip-> prod-host-zipper
                            :instance)) ;; -> ("prod-host/instance2" "prod-host/instance")


