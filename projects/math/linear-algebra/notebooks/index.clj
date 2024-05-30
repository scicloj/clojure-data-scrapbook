(ns index
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
             [random :refer [rand-uniform!]]]
            [scicloj.kindly.v4.kind :as kind]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]))




(dv [1 39 17 31])




(dge [[1 34 4]
      [31 4 5]
      [1 4 1]])




(mv
 (dge [[1 34 4]
       [31 4 5]
       [1 4 1]])
 (dv [3 1 4]))












(def origin (dv 0 0))

(def v1 (dv 1 2))

(def v2 (dv -1 3))


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


(plot-vs [v1 v2])

(def m1 (dge 2 2))
(copy! (dv 1 0) (row m1 0))
(copy! (dv 0 2) (row m1 1))

m1


(plot-vs [(mv m1 v1)
          (mv m1 v2)])



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
        [:h1 "------>"]]
       [:div {:style {:display "inline-block"}}
        (kind/vega-lite
         (plot-vs new-vs
                  {:xscale {:domain xdomain}
                   :yscale {:domain ydomain}}))]]])))


(plot-change (partial mv m1) [v1 v2])



(def v1+v2 (axpy v1 v2))

(def v1*3 (scal 3 v1))

(plot-vs [v1 v2 v1+v2])

(plot-vs [v1 v1*3])

(plot-vs [v1 v2 v1+v2 v1*3])


(defn f1 [v]
  (dv (v 0)
      (* 2 (v 1))))


(f1 (dv 1 2))


;; Intel MKL


;; parallelogram

(plot-change f1 [v1 v2 v1+v2 v1*3])



;; f1 is a linear tranformation

v1

v2

v1+v2


;; f1 is additive:

(f1 (axpy v1
          v2))

(axpy
 (f1 v1)
 (f1 v2))

;; f1 respects scaling

(f1 (scal 3 v1))

(scal 3 (f1 v1))


;; f1 is linear -- additive and respects scaling


;; translation is not considered linear
;; but rather "affine"


(defonce random-vectors
  (repeatedly
   4
   (fn []
     (dv (rand)
         (rand)))))


random-vectors

(plot-vs random-vectors)


(plot-change f1 random-vectors)


;; nonlinear

(defn f2 [v]
  (dv (v 0)
      (* (v 1) (v 1))))


(plot-change f2 random-vectors)


;;

(def m1
  (dge 2 2 [1 2
            3 4]
       {:layout :row}))

m1


;; (def m2
;;   (new-2x1-matrix 1 2))

(def m2 (dge 2 1 [1 2]))

m2


(mm m1 m2)



;; m1 2x2
;; m2 2x1







(mv m1 (dv 0 0))

(mv m1 (dv 1 0)) ; 1st column of m1

(mv m1 (dv 0 1)) ; 2nd column of m1

(mv m1 (dv 10 100)) ; 10 times 1st column plus 100 times 2nd column




(plot-change (fn [v] (mv m1 v))
             random-vectors)





(plot-change (fn [v] (mv
                      (dge 2 2 [1 0
                                0 2])
                      v))
             random-vectors)


(plot-change (fn [v] (mv
                      (dge 2 2 [-3 0
                                0 2])
                      v))
             random-vectors)


(defn rot [theta]
  (dge 2 2 [(cos theta) (- (sin theta))
            (sin theta) (cos theta)]
       {:layout :row}))


(plot-change #(mv (rot 0.2) %)
             random-vectors)


(defn rescale-y [scale]
  (dge 2 2 [1 0
            0 scale]))


(plot-change #(mv (rescale-y 2) %)
                  random-vectors)



(plot-change (comp #(mv (rot 0.2) %)
                        #(mv (rescale-y 2) %))
                  random-vectors)



(plot-change (comp #(mv (rescale-y 2) %)
                        #(mv (rot 0.2) %))
                  random-vectors)

(plot-change #(mv (mm (rescale-y 2)
                           (rot 0.2))
                       %)
                  random-vectors)



(rescale-y 2)


(rot 0.2)


(mm (rescale-y 2)
    (rot 0.2))
