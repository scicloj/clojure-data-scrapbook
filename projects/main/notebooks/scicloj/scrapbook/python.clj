;; # Python interop

(ns scicloj.scrapbook.python
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.python :as vis.python]
            [libpython-clj2.require :refer [require-python]]
            [libpython-clj2.python :refer [py. py.. py.-] :as py]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly.v4.kind :as kind]
            [hiccup.core :as hiccup]
            hiccup.util))


;; ## Using Python visualizations

(require-python '[numpy :as np]
                '[numpy.random :as np.random]
                'matplotlib.pyplot
                '[seaborn :as sns]
                'json)

(def sine-data
  (-> {:x (range 0 (* 3 np/pi) 0.1)}
      tc/dataset
      (tc/add-column :y #(fun/sin (:x %)))))

(vis.python/with-pyplot
  ;; http://gigasquidsoftware.com/blog/2020/01/18/parens-for-pyplot/
  (matplotlib.pyplot/plot
   (:x sine-data)
   (:y sine-data)))

(vis.python/pyplot
 #(matplotlib.pyplot/plot
   (:x sine-data)
   (:y sine-data)))

;; https://seaborn.pydata.org/tutorial/introduction
(let [tips (sns/load_dataset "tips")]
  (sns/set_theme)
  (vis.python/pyplot
   #(sns/relplot :data tips
                 :x "total_bill"
                 :y "tip"
                 :col "time"
                 :hue "smoker"
                 :style "smoker"
                 :size "size")))
