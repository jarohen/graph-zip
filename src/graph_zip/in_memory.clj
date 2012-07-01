(ns graph-zip.in-memory
   (:use [graph-zip.core])
   (:require [clojure.zip :as zip]
             [clojure.xml :as xml]
             [clojure.data.zip :as zf]))

(defrecord InMemoryGraph [graphs]
    Graph
    (props-map [this node direction]
      (or (get-in this [:graphs direction node])
          {}))
    (prop-values [this node prop direction]
      (let [props (props-map this node direction)]
        (or (get props prop)
            []))))

(defn- add-statement-to-map [graph-map from property to]
  (assoc graph-map from
         (let [props (get graph-map from)]
           (assoc props property
                  (conj (get props property) to)))))

(defn- add-statement-to-maps [{:keys [in out]} {:keys [subject property object]}]
  {:in (add-statement-to-map in object property subject)
   :out (add-statement-to-map out subject property object)})

;; statements :: [{:subject :property :object}]
(defn build-in-memory-graph
  ([statements]
     (build-in-memory-graph nil statements))
  ([graph statements]
     
     (InMemoryGraph. (reduce add-statement-to-maps (:graphs graph) statements))))

;; ----------- TESTS

(def my-map (build-in-memory-graph [{:subject "prod-host" :property :instance :object "prod-host/instance"}
                                    {:subject "prod-host" :property :instance :object "prod-host/instance2"}
                                    {:subject "prod-host/instance" :property :userid :object "my-user"}
                                    {:subject "prod-host/instance" :property "label" :object "1"}
                                    {:subject "prod-host/instance2" :property "label" :object "2"}
                                    {:subject "prod-host/instance" :property "jvm" :object "prod-host/instance/jvm"}
                                    {:subject "prod-host/instance/jvm" :property "maxMem" :object "1024m"}]))

(def prod-host-zipper (graph-zipper my-map "prod-host"))

(zipper-node (zip1-> prod-host-zipper
                     :instance
                     [(prop= "label" "1")]
                     "jvm"
                     "maxMem")) ;; -> "1024m"

(zipper-node (zip1-> prod-host-zipper
                     :instance
                     (prop= "label" "2"))) ;; -> "prod-host/instance2"

(map zipper-node (zip-> prod-host-zipper
                        :instance)) ;; -> ("prod-host/instance2" "prod-host/instance")

(zipper-node (zip1-> (graph-zipper my-map "prod-host/instance")
                     (incoming :instance))) ;; -> "prod-host"


