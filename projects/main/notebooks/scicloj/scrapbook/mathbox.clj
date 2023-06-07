;; # Mathbox.cljs

(ns scicloj.scrapbook.mathbox
  (:require [scicloj.kindly.v3.kind :as kind]))

;; inspired by [the official MathBox.cljs tutorial](https://mathbox.mentat.org/)
(kind/hiccup
 '[(let [Data (fn []
                [mathbox.primitives/Interval
                 {:expr (fn [emit x _i t]
                          (emit x (Math/sin (+ x t))))
                  :width 64
                  :channels 2}])
         Curve (fn []
                 [:<>
                  [Data]
                  [mathbox.primitives/Line {:width 5
                                            :color "#3090FF"}]])
         Main (fn []
                [mathbox.core/MathBox
                 {:container {:style {:height "400px" :width "100%"}}
                  :focus 3}
                 [mathbox.primitives/Camera {:position [0 0 3]
                                             :proxy true}]
                 [mathbox.primitives/Cartesian
                  {:range [[-2 2] [-1 1]]
                   :scale [2 1]}
                  [mathbox.primitives/Axis {:axis 1 :width 3 :color "black"}]
                  [mathbox.primitives/Axis {:axis 2 :width 3 :color "black"}]
                  [mathbox.primitives/Grid {:width 2 :divideX 20 :divideY 10}]
                  [Curve]]])
         *state (r/atom {:open? false})]
     (fn []
       [:div
        [:button {:on-click #(swap! *state update :open? not)}
         (if (:open? @*state)
           "close"
           "open")]
        (when (:open? @*state)
          [Main])]))])
