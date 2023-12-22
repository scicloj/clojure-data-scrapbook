
^{:kindly/hide-code? true
  :kindly/kind :kind/hiccup}
[:table
 [:tr
  [:td "This is part of the Scicloj "
   [:a {:href "https://scicloj.github.io/clojure-data-scrapbook/"}
    "Clojure Data Scrapbook"]
   "."]
  [:a
   {:href "https://scicloj.github.io/clojure-data-scrapbook/"}
   [:img {:src "https://scicloj.github.io/sci-cloj-logo-transparent.png"
          :alt "SciCloj logo"
          :width "40"
          :align "left"}]]]]

;; # Clay & Noj demo: data visualization

(ns index
  (:require [scicloj.noj.v1.datasets
             :as datasets]
            [scicloj.noj.v1.vis.hanami
             :as hanami]
            [scicloj.kindly.v4.kind
             :as kind]))

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
