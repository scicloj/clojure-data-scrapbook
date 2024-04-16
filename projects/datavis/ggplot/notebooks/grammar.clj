(ns grammar
  (:require [tablecloth.api :as tc]
            [scicloj.metamorph.ml.toydata.ggplot :as toydata.ggplot]
            [tech.v3.datatype.functional :as fun]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]
            [clojure.math :as math]
            [clojure2d.color :as color]
            [ggplotly-cont]
            [tablecloth.column.api :as tcc]))

;; # Exploging a ggplot-like grammar - DRAFT

(defn factor [column]
  (-> column
      (vary-meta
       (fn [m]
         (-> m
             (assoc :gg/factor? true)
             (assoc :name (->> column
                               meta
                               :name
                               name
                               (format "factor(%s)"))))))))

(defn factor? [column]
  (:gg/factor? column))

(defn add-column-by-name [ds new-column-fn]
  (let [new-column (new-column-fn ds)]
    (-> ds
        (tc/add-column (-> new-column meta :name)
                       new-column))))

(-> toydata.ggplot/mpg
    (add-column-by-name #(factor (:cyl %)))
    (tc/group-by ["factor(cyl)"] {:result-type :as-map}))


(-> {:data toydata.ggplot/mpg}
    ((fn [{:keys [data]}]
       (let [grouping-columns ["factor(cyl)"]
             columns-for-text [:hwy :disply "factor(cyl)"]
             point-layers (-> data
                              (add-column-by-name #(factor (:cyl %)))
                              (tc/group-by grouping-columns {:result-type :as-map})
                              (->> (sort-by (comp pr-str key))
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
                                                              (ggplotly-cont/texts columns-for-text)))
                                             ggplotly-cont/layer)
                                         (-> base
                                             (assoc :type :line
                                                    :showlegend false
                                                    :y predictions
                                                    :text (-> group-data
                                                              ;; (tc/add-column :displ predictions)
                                                              (ggplotly-cont/texts columns-for-text)))
                                             ggplotly-cont/layer)])))
                                   (apply concat)))
             xmin (-> data :hwy tcc/reduce-min)
             xmax (-> data :hwy tcc/reduce-max)
             ymin (-> data :displ tcc/reduce-min)
             ymax (-> data :displ tcc/reduce-max)
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
           :jsHooks []})))))
