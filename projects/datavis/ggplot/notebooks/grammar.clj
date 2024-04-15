(ns grammar
  (:require [tablecloth.api :as tc]
            [scicloj.metamorph.ml.toydata.ggplot :as toydata.ggplot]
            [tech.v3.datatype.functional :as fun]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]
            [clojure.math :as math]
            [clojure2d.color :as color]
            [ggplotly-cont]))

;; # Exploging a ggplot-like grammar - DRAFT

(defn factor [column]
  (vary-meta column
             assoc :gg/factor? true))

(defn factor? [column]
  (:gg/factor? column))



(let [data toydata.ggplot/mpg
      point-layers (-> data
                       (tc/add-column "factor(cyl)" #(factor (:cyl %)))
                       (tc/group-by :cyl {:result-type :as-map})
                       (->> (sort-by key)
                            (map-indexed
                             (fn [i [group-name group-data]]
                               (let [base {:x (:hwy group-data),
                                           :y (:displ group-data)
                                           :color (ggplotly-cont/colors i)
                                           :name group-name
                                           :legendgroup group-name}
                                     predictions (map
                                                  (fun/linear-regressor (:hwy group-data)
                                                                        (:displ group-data))
                                                  (:hwy group-data))]
                                 [(-> base
                                      (assoc :type :point
                                             :showlegend true
                                             :y (:displ group-data)
                                             :text (-> group-data
                                                       (ggplotly-cont/texts [:hwy :displ "factor(cyl)"])))
                                      ggplotly-cont/layer)
                                  (-> base
                                      (assoc :type :line
                                             :showlegend false
                                             :y predictions
                                             :text (-> group-data
                                                       ;; (tc/add-column :displ predictions)
                                                       (ggplotly-cont/texts [:hwy :displ "factor(cyl)"])))
                                      ggplotly-cont/layer)])))
                            (apply concat)))
      xmin (-> data :hwy fun/reduce-min)
      xmax (-> data :hwy fun/reduce-max)
      ymin (-> data :displ fun/reduce-min)
      ymax (-> data :displ fun/reduce-max)
      xaxis (ggplotly-cont/axis {:minval xmin
                                 :maxval xmax
                                 :anchor "x"
                                 :title :hwy})
      yaxis (ggplotly-cont/axis {:minval ymin
                                 :maxval ymax
                                 :anchor "x"
                                 :title :displ})]
  (kind/htmlwidgets-ggplotly
   {:x
    {:config ggplotly-cont/config
     :highlight ggplotly-cont/highlight
     :base_url "https://plot.ly",
     :layout (ggplotly-cont/layout {:xaxis xaxis
                                    :yaxis yaxis})
     :data point-layers},
    :evals [],
    :jsHooks []}))
