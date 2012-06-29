You can use graph zip like this

    (use 'graph-zip.core :only [graph-zip])
    (use 'graph-zip.in-memory :only [build-in-memory-graph])

    (graph-> (graph-zip (build-in-memory-graph edges))
        :A :B)
