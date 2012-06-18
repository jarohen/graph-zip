(ns graph-zip.core
  (:require [clojure.zip :as zip]))


(defn graph-branch? [_] true)

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
  (reduce
   #(if-let [{:keys [subject property object]} %2]
             (assoc %1 subject
                    (if-let [existing-props (find %1 subject)]
                      (cons (val existing-props) [[property object]])
                      [property object]))
             %1)
      (hash-map)
      statements))

  
