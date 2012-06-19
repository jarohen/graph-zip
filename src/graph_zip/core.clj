(ns graph-zip.core
  (:require [clojure.zip :as zip]))

(defn- add-statement-to-map [graph-map {:keys [subject property object]}]
  (assoc graph-map subject
          (if-let [existing-props (get graph-map subject)]
            (cons [property object] existing-props)
            [[property object]])))

(defn- graph-branch? [_] true)

(defn graph-children? [graph-map]
  (fn [[_ uri]]
    (if-let [entry (find graph-map uri)]
      (val entry)
      []))
  )

;; graph-map :: {subject -> [property object]}
(defn graph-zipper [graph-map root-subject]
  (zip/zipper
   graph-branch?
   (graph-children? graph-map)
   nil ;;This is the make-node function, not entirely sure what this does...
   [nil root-subject]))

;; statements :: [{:subject :property :object}]
(defn build-graph-map [statements]
  (reduce add-statement-to-map
      (hash-map)
      statements))

  
