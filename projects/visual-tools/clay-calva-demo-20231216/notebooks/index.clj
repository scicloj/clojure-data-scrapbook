(load-file "../../../header.edn")

;; # Clay & Noj demo: data visualization

(ns index
  (:require [scicloj.noj.v1.datasets
             :as datasets]
            [scicloj.noj.v1.vis.hanami
             :as hanami]
            [scicloj.kindly.v4.kind
             :as kind]))

(kind/video {:youtube-id "X_SsjhmG5Ok"})

;; ## Arithmetic

(+ 1 2)

;; ## Datasets

datasets/iris

;; ## Tables

(-> datasets/iris
    kind/table)

;; ## Visualization

(-> datasets/iris
    (hanami/linear-regression-plot
     :petal-length
     :petal-width
     {:line-options {:MCOLOR "brown"
                     :MSIZE 10
                     :OPACITY 0.5}
      :point-options {:MSIZE 100}}))
