# Graph-Zip

Graph zip is a zipper library for Clojure that can navigate graph
structures, with a similar syntax to xml-zip.

## Lein dependency:

    [graph-zip "0.5"]

## Usage
You can use graph zip like this:

### Building a graph
    ;; Dependencies for an in-memory graph
    (use 'graph-zip.core :only [graph-zip zip-> zip1-> zipper-node prop=])
    (use 'graph-zip.in-memory :only [build-in-memory-graph])


    ;; Build up the in-memory graph - a list of triples, each with a :subject, :property and :object
    (def my-map (build-in-memory-graph 
                  [{:subject "prod-host" :property :instance :object "prod-host/instance"}
                   {:subject "prod-host" :property :instance :object "prod-host/instance2"}
                   {:subject "prod-host" :property :instance :object "prod-host/instance3"}
                   {:subject "prod-host/instance" :property :userid :object "my-user"}
                   {:subject "prod-host/instance2" :property :userid :object "another-user"}
                   {:subject "prod-host/instance" :property "label" :object "1"}
                   {:subject "prod-host/instance2" :property "label" :object "2"}
                   {:subject "prod-host/instance" :property "cmdb:jvm" :object "prod-host/instance/jvm"}
                   {:subject "prod-host/instance/jvm" :property "cmdb:maxMem" :object "1024m"}]))


### Building a zipper

A zipper is a pair of the full graph, and the current node in the
traversal. We can build a re-usable zipper, rooted at the "prod-host"
node

    (def prod-host-zipper (graph-zip my-map "prod-host"))

This 'zips' along the ```:instance``` relation, and returns a list of
zippers.

Mapping zipper-node over the zippers returns just the nodes that are
related to "prod-host" by ```:instance```

    (zip-> prod-host-zipper
           :instance
           node) ;; -> ("prod-host/instance2" "prod-host/instance")

In a similar vein to xml-zip, you can prune the graph by using a
```prop=``` predicate.

If you are only expecting one result, you can use the ```zip1->``` function
instead.

    (zip1-> prod-host-zipper
            :instance
            (prop= "label" "2")
            node) ;; -> "prod-host/instance2"


Relations don't have to be keywords

    (zip1-> prod-host-zipper
            :instance
            [(prop= "label" "1")]
            "jvm"
            "maxMem"
            node) ;; -> "1024m"

Get the properties map of a node using ```props```

    (zip1-> prod-host-zipper
            :instance
            (prop= "label" "1")
            props) 
            ;; -> {"jvm" ("prod-host/instance/jvm"), 
                   "label" ("1"), 
                   :userid ("my-user")}


### Merging graphs

Graph-Zip can merge graphs from many sources. Here we are just merging
two in-memory graphs:

    (use 'graph-zip.merge-graph :only [make-merge-graph])

    (def additional-map 
     (build-in-memory-graph 
      [{:subject "prod-host" :property :instance :object "prod-host/instance3"}
       {:subject "prod-host" :property "cmdb:hostname" :object "prod-server.example.com"}
       {:subject "prod-host/instance3" :property :userid :object "my-user"}
       {:subject "prod-host/instance3" :property "label" :object "3"}]))   
                                                
    (def merged-graph (make-merge-graph my-map additional-map))
    (def merged-zipper (graph-zip merged-graph "prod-host"))

The merge behaves as you'd expect:

    (zip1-> merged-zipper
            :instance
            (prop= "label" "3")
            node) 
    ;; -> "prod-host/instance3"

    (zip1-> merged-zipper
            :instance
            (prop= "label" "1")
            node) 
    ;; -> "prod-host/instance"

    (zip-> merged-zipper
           :instance
           node)
    ;; -> ("prod-host/instance3" 
           "prod-host/instance2"
           "prod-host/instance")


### Traversing back up the graph

Since v0.3, it is possible to traverse back up the graph as well, with ```(incoming :property)```:

Find all the instances where the userid is ```my-user```:

```go-to``` moves the zipper to the given node.

    (zip-> merged-zipper
           (go-to "my-user")
           (incoming :userid))) 
    ;; -> ("prod-host/instance3" 
           "prod-host/instance")
                            

# Licence

Copyright (C) 2012 James Henderson, Malcolm Sparks

Distributed under the Eclipse Public License, the same as Clojure.


    

    


