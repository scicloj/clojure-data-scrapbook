(ns intro
  (:require [uncomplicate.commons.core :refer [with-release]]
            [uncomplicate.fluokitten.core :refer [foldmap]]
            [uncomplicate.clojurecl.core :as opencl]
            [uncomplicate.neanderthal
             [core :refer [dot copy asum copy! row mv mm rk axpy entry!
                           subvector trans mm! zero
                           scal]]
             [vect-math :refer [mul]]
             [native :refer [dv dge fge]]
             [opencl :as cl :refer [clv]]
             [random :refer [rand-uniform!]]
             [math :refer [cos sin sqrt]]]
            [scicloj.kindly.v4.kind :as kind]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]))


(def origin (dv 0 0))

(defn plot-vs
  ([vs] (plot-vs vs nil))
  ([vs {:keys [xscale yscale]}]
   (let [points (->> vs
                     (cons origin)
                     (map-indexed (fn [i v]
                                    {:x (v 0)
                                     :y (v 1)
                                     :c i})))
         segments (->> points
                       (map (fn [point]
                              (merge point
                                     {:x2 0
                                      :y2 0}))))]
     (-> ht/layer-chart
         (hc/xform :DATA segments
                   :LAYER [(hc/xform ht/point-chart
                                     :SIZE 1000
                                     :COLOR "c"
                                     :XSCALE (or xscale hc/RMV)
                                     :YSCALE (or yscale hc/RMV))
                           (hc/xform ht/rule-chart
                                     :SIZE 5
                                     :COLOR "c"
                                     :XSCALE (or xscale hc/RMV)
                                     :YSCALE (or yscale hc/RMV)
                                     :X2 :x2
                                     :Y2 :y2)])
         kind/vega-lite))))

(defn plot-change [f vs]
  (let [new-vs (mapv f vs)
        all-vs (concat [origin] vs new-vs)
        xdomain (->> all-vs
                     (map #(% 0))
                     ((juxt (partial apply min)
                            (partial apply max))))
        ydomain (->> all-vs
                     (map #(% 1))
                     ((juxt (partial apply min)
                            (partial apply max))))]
    (kind/hiccup
     [:div
      [:div {:style {:display "flex"}}
       [:div {:style {:display "inline-block"}}
        (kind/vega-lite
         (plot-vs vs
                  {:xscale {:domain xdomain}
                   :yscale {:domain ydomain}}))]
       [:div {:style {:display "inline-block"
                      :vertical-align "middle"}}
        [:h1 [:big [:big ">"]]]]
       [:div {:style {:display "inline-block"}}
        (kind/vega-lite
         (plot-vs new-vs
                  {:xscale {:domain xdomain}
                   :yscale {:domain ydomain}}))]]])))


(plot-vs
 [(dv [-3 9])
  (dv [12 -20])])


;; a team of x programmers
;; we manage to produce y lines-of-code

{:linear (kind/plotly
          {:data [{:y (for [x (range 20)]
                        (* 1000 x))
                   :type :line}]})

 :affine (kind/plotly
          {:data [{:y (for [x (range 20)]
                        (+ 20000 (* 1000 x)))
                   :type :line}]})

 :nonlinear (kind/plotly
             {:data [{:y (for [x (range 20)]
                           (* 1000 (sqrt x)))
                      :type :line}]})}


;; a team of x programmers
;; and y product managers
;; we manage to produce z lines-of-code

(kind/plotly
 {:data [{:z (for [x (range 6)]
               (for [y (range 6)]
                 (+ (* 1000 x)
                    (* 3000 y))))
          :type :surface}]})

;; dim 2 ----> dim 1

;; it is a linear mapping:
;; it takes (0,0)-->0
;; it takes parallel lines to parallel lines

(kind/plotly
 {:data [{:z (for [x (range 6)]
               (for [y (range 6)]
                 (+ (* 1000 (sqrt x))
                    (* 3000 (sqrt y)))))
          :type :surface}]})


(kind/plotly
 {:data [{:z (for [x (range 6)]
               (for [y (range 6)]
                 (+ (* 1000 (sqrt x))
                    (* 3000 (sqrt y))
                    (* 3000 y (sqrt x)))))
          :type :surface}]})

;; in our team, each day,
;; a team-member might switch their role

;; 2 dim ---> 2 dim

(defn expected-tomorrow [[x y]]
  [(+ (* 0.9 x) (* 0.1 y))
   (+ (* 0.1 x) (* 0.9 y))])

(take 20 (iterate expected-tomorrow [3 9]))

(def expected-tomorrow-matrix
  (dge [[0.9 0.1]
        [0.1 0.9]]))

(mv expected-tomorrow-matrix
    (dv [3 9]))

(expected-tomorrow
 [3 9])


(let [initial-state-1 (dv [3 9])
      initial-state-2 (dv [13 9])]
  (plot-vs [initial-state-1
            initial-state-2]))

(let [initial-state-1 (dv [3 9])
      initial-state-2 (dv [13 9])]
  (plot-vs (mapv #(mv expected-tomorrow-matrix %)
                 [initial-state-1
                  initial-state-2])))




(let [initial-state-1 (dv [3 9])
      initial-state-2 (dv [13 9])]
  (plot-change #(mv expected-tomorrow-matrix
                    (mv expected-tomorrow-matrix
                        (mv expected-tomorrow-matrix
                            (mv expected-tomorrow-matrix %))))
               [initial-state-1
                initial-state-2]))


(mv m1 (mv m2 (mv m3 (mv m4
                         v))))

(mv (mm m1 (mm m2 (mm m3 m4)))
    v)


(let [initial-state-1 (dv [3 9])
      initial-state-2 (dv [13 9])
      expected-tomorrow-4 (mm expected-tomorrow-matrix
                              (mm expected-tomorrow-matrix
                                  (mm expected-tomorrow-matrix
                                      expected-tomorrow-matrix)))]
  (plot-change #(mv expected-tomorrow-4 %)
               [initial-state-1
                initial-state-2]))



(defn mm-by-itself [m]
  (mm m m))



(let [initial-state-1 (dv [3 9])
      initial-state-2 (dv [13 9])
      expected-tomorrow-4 (mm-by-itself (mm-by-itself expected-tomorrow-matrix))]
  (plot-change #(mv expected-tomorrow-4 %)
               [initial-state-1
                initial-state-2]))


mv: 2x2 matrix, 2 vector ---> 2 vector

mm: 2x2 matrix, 2x2 matrix ---> 2x2 matrix


mv is defined so that it would fit our "expected tomorrow formula" -- just a way to implement any linear transformation
linear transformations 2 dim --> 2 dim are implemented by 2x2

mm is defined so that matrix multiplication would correspond to composition of transformations







(let [initial-state (dv (repeat 5 1/5))
      transition (dge [[9/10 1/10 0 0 0]
                       [0 9/10 1/10 0 0]
                       [0 0 9/10 1/10 0]
                       [0 0 0 9/10 1/10]
                       [1/20 0 1/20 0 9/10]])]
  (mv ((->> square-mm
            (repeat 10)
            (apply comp))
       transition)
      initial-state))




(kind/cytoscape
 {:elements {:nodes (for [i (range 5)]
                      {:data {:id i}})
             :edges (concat (for [i (range 4)]
                              {:data {:source i :target (inc i)}})
                            [{:data {:source 4 :target 0}}
                             {:data {:source 4 :target 2}}])}
  :style [{:selector :edge
           :style {:targetArrowShape :triangle
                   :curveStyle :bezier
                   :width 10}}
          {:selector :node
           :css {:content "data(id)"}}]
  :layout {:name :grid}})










(let [initial-state (dv (repeat 5 1/5))
      transition (dge [[9/10 1/10 0 0 0]
                       [0 9/10 1/10 0 0]
                       [0 0 9/10 1/10 0]
                       [0 0 0 9/10 1/10]
                       [1/20 0 1/20 0 9/10]])]
  (mv ((->> mm-by-itself
            (repeat 10)
            (apply comp))
       transition)
      initial-state))
