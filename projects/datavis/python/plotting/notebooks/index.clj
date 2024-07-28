(load-file "../../../../header.edn")

;; ---------------

;; This notebook demonstrates a self-contained workflow for visualizing Python plots in current Clojure tooling using the [Kindly](https://scicloj.github.io/kindly/) convention.

;; The only dependency necessary is the [Libpython-clj](https://github.com/clj-python/libpython-clj) bridge. Some Kindly-compatible tool is needed to make the visualization visible. This was rendered using [Clay](https://scicloj.github.io/clay/) as an extra dev dependency.

;; The implementation is inspired by the [Parens for Pyplot](https://gigasquidsoftware.com/blog/2020/01/18/parens-for-pyplot/) tutorial by Carin Meier from Jan 2020. It has been part of the [Noj](https://scicloj.github.io/noj/) library till version `1-alpha34`, but as of July 2024, it is part of a dedicated library, [Kind-pyplot](https://scicloj.github.io/kind-pyplot).

;; ## Setup

;; Let us require the relevant namespaces from Libpyton-clj:

(ns index
  (:require [libpython-clj2.require :refer [require-python]]
            [libpython-clj2.python :refer [py. py.. py.-] :as py]))

;; Now we can require the relevant Python modules from [Matplotlib](https://matplotlib.org/):

(require-python 'matplotlib.pyplot
                'matplotlib.backends.backend_agg)

;; ## Implementation

(defmacro with-pyplot
  "Takes forms with mathplotlib.pyplot and returns a showable (SVG) plot.
  E.g.:

  (with-pyplot
    (matplotlib.pyplot/plot
     [1 2 3]
     [1 4 9]))
  "
  [& forms]
  `(let [_# (matplotlib.pyplot/clf)
         fig# (matplotlib.pyplot/figure)
         agg-canvas# (matplotlib.backends.backend_agg/FigureCanvasAgg fig#)
         path# (.getAbsolutePath
                (java.io.File/createTempFile "plot-" ".svg"))]
     ~(cons 'do forms)
     (py. agg-canvas# "draw")
     (matplotlib.pyplot/savefig path#)
     ;; Take the SVG file path and turn it into
     ;; a Clojure value that can be displayed in Kindly-compatible tools.
     (-> path#
         slurp
         vector
         (with-meta {:kindly/kind :kind/html}))))

(defn pyplot
  "Takes a function plotting using mathplotlib.pyplot, and returns a showable (SVG) plot.
  E.g.:

  (pyplot
    #(matplotlib.pyplot/plot
      [1 2 3]
      [1 4 9]))
  "
  [plotting-function]
  (with-pyplot
    (plotting-function)))

;; ## Examples

;; From the Parens for Pyplot blogpost:

(require-python '[numpy :as np])

(require '[clojure.math :as math])

(def sine-data
  (let [x (range 0 (* 3 np/pi) 0.1)]
    (-> {:x (vec x)
         :y (mapv math/sin x)})))

(with-pyplot
  (matplotlib.pyplot/plot
   (:x sine-data)
   (:y sine-data)))

(pyplot
 #(matplotlib.pyplot/plot
   (:x sine-data)
   (:y sine-data)))

;; From the [Seaborn intro](https://seaborn.pydata.org/tutorial/introduction):

(require-python '[seaborn :as sns])

(let [tips (sns/load_dataset "tips")]
  (sns/set_theme)
  (pyplot
   #(sns/relplot :data tips
                 :x "total_bill"
                 :y "tip"
                 :col "time"
                 :hue "smoker"
                 :style "smoker"
                 :size "size")))
