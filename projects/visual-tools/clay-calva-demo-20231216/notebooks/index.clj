(load-file "../../../header.edn")

;; # Clay & Noj demo: data visualization

(ns index
  (:require [scicloj.metamorph.ml.toydata
             :as toydata]
            [scicloj.noj.v1.vis.hanami
             :as hanami]
            [scicloj.kindly.v4.kind
             :as kind]))

(kind/video {:youtube-id "X_SsjhmG5Ok"})

;; ## Arithmetic

(+ 1 2)

;; ## Datasets

(def iris
  (toydata/iris-ds))

iris

;; ## Tables

(-> iris
    (kind/table {:element/max-height "500px"}))

;; ## Visualization

(-> iris
    (hanami/linear-regression-plot
     :petal_length
     :petal_width
     {:line-options {:MCOLOR "brown"
                     :MSIZE 10
                     :OPACITY 0.5}
      :point-options {:MSIZE 100}}))
