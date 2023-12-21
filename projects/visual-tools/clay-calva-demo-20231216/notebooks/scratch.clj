(ns scratch
  (:require [scicloj.noj.v1.datasets
             :as datasets]
            [scicloj.noj.v1.vis.hanami
             :as hanami]
            [scicloj.kindly.v4.kind
             :as kind]))

;; # Intro

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

