(load-file "../../../header.edn")

(ns study-session-20240317
  (:require [clojisr.v1.r :as r :refer [r]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [scicloj.metamorph.ml.toydata :as toydata]
            [scicloj.metamorph.ml.toydata.ggplot :as toydata.ggplot]
            [scicloj.tempfiles.api :as tempfiles]
            [representing]))

;; # Code examples for study session 2024-03-17

;; DRAFT

(run! r/library ["ggplot2" "plotly"])

(r/require-r '[ggplot2 :as gg])
(r/require-r '[plotly :as plotly])
(r/require-r '[htmlwidgets :as htmlwidgets])

(defn ggplotly [ggplot]
  (let [{:keys [path]} (tempfiles/tempfile! ".html")]
    (-> ggplot
        plotly/ggplotly
        (htmlwidgets/saveWidget path))
    (kind/hiccup
     [:iframe {:height "500px"
               :width "500px"
               :srcdoc (slurp path)}])))

toydata.ggplot/midwest


(-> toydata.ggplot/midwest
    (gg/ggplot (gg/aes 'area 'poptotal
                       :color 'state
                       :label 'county
                       :size 20
                       :alpha 0.6))
    (r/r+ (gg/geom_point)
          (gg/scale_y_log10)
          (gg/scale_color_brewer :palette "Set2"))
    ggplotly)
