(ns graph-zip.in-memory
   (:use [graph-zip.core])
   (:require [clojure.zip :as zip]
             [clojure.xml :as xml]
             [clojure.data.zip :as zf]))

(defrecord InMemoryGraph [graph-map]
    Graph
    (props-map [graph node]
      (get (:graph-map graph) node))
    (prop-values [graph node prop]
      (let [props (props-map graph node)]
        (get props prop)))
    (to-datalog-db [graph _] nil)) ;;todo

(defn- add-statement-to-map [graph-map {:keys [subject property object]}]
  (assoc graph-map subject
         (let [props (get graph-map subject)]
           (assoc props property
                  (conj (get props property) object)))))

;; statements :: [{:subject :property :object}]
(defn build-in-memory-graph
  ([statements]
     (build-in-memory-graph nil statements))
  ([graph statements]
     (InMemoryGraph. (reduce add-statement-to-map (:graph-map graph) statements))))

;; ----------- TESTS

(def my-map (build-in-memory-graph [{:subject "patbox" :property :instance :object "patbox/instance"}
                          {:subject "patbox" :property :instance :object "patbox/instance2"}
                          {:subject "patbox/instance" :property :userid :object "mis"}
                          {:subject "patbox/instance" :property "label" :object "1"}
                          {:subject "patbox/instance2" :property "label" :object "2"}
                          {:subject "patbox/instance" :property "cmdb:jvm" :object "patbox/instance/jvm"}
                          {:subject "patbox/instance/jvm" :property "cmdb:maxMem" :object "1024m"}]))

(def patbox-loc (graph-zipper my-map "patbox"))

(loc-node (graph1-> patbox-loc
                        :instance
                        [(prop= "label" "1")]
                        "cmdb:jvm"
                        "cmdb:maxMem")) ;; -> "1024m"

(loc-node (graph1-> patbox-loc
                        :instance
                        (prop= "label" "1"))) ;; -> "patbox/instance2"

(map loc-node (graph-> patbox-loc
                       :instance)) ;; -> ("patbox/instance2" "patbox/instance")

