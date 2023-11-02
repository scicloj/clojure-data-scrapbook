;; # Data visualization

(ns scicloj.scrapbook.visualization
  (:require [tablecloth.api :as tc]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.noj.v1.vis :as vis]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly.v4.kind :as kind]
            [hiccup.core :as hiccup]
            hiccup.util))


(def dataset1
  (let [n 19]
    (tc/dataset
     {:x (range n)
      :y (map +
              (range n)
              (repeatedly n rand))})))

dataset1

;; ## Visualizing datases with Hanami
(kind/vega-lite
 (hc/xform ht/point-chart
           :DATA (-> dataset1
                     (tc/rows :as-maps)
                     vec)
           :MSIZE 200))

;; ## Visualizing datases with Hanami using Noj
(-> dataset1
    (vis/hanami-plot ht/point-chart
                     :MSIZE 200))
