(ns index
  (:require [libpython-clj2.require :refer [require-python]]
            [libpython-clj2.python :refer [py. py.. py.-] :as py]
            [clojure.math :as math]))


;; Inspiration: the [Parens for Pyplot](http://gigasquidsoftware.com/blog/2020/01/18/parens-for-pyplot/) blog post at Squid's blog.

(require-python 'matplotlib.pyplot
                'matplotlib.backends.backend_agg
                'numpy)

(defmacro with-pyplot
  "Takes forms with mathplotlib.pyplot and returns a showable (SVG) plot."
  [& body]
  `(let [_# (matplotlib.pyplot/clf)
         fig# (matplotlib.pyplot/figure)
         agg-canvas# (matplotlib.backends.backend_agg/FigureCanvasAgg fig#)
         path# (.getAbsolutePath
                (java.io.File/createTempFile "plot-" ".svg"))]
     ~(cons 'do body)
     (py. agg-canvas# "draw")
     (matplotlib.pyplot/savefig path#)
     (-> path#
         slurp
         vector
         (with-meta {:kindly/kind :kind/html}))))

(defn pyplot
  "Takes a function plotting using mathplotlib.pyplot, and returns a showable (SVG) plot"
  [plotting-function]
  (let [_ (matplotlib.pyplot/clf)
        fig (matplotlib.pyplot/figure)
        agg-canvas (matplotlib.backends.backend_agg/FigureCanvasAgg fig)
        path (.getAbsolutePath
              (java.io.File/createTempFile "plot-" ".svg"))]
    (plotting-function)
    (py. agg-canvas "draw")
    (matplotlib.pyplot/savefig path)
    (-> path
        slurp
        vector
        (with-meta {:kindly/kind :kind/html}))))



(require-python '[numpy :as np]
                '[numpy.random :as np.random]
                '[seaborn :as sns])

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

;; https://seaborn.pydata.org/tutorial/introduction
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
