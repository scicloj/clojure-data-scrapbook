(ns notebooks.commits
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis :as vis]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [data.generate-dataset]
            [clojure.math :as math]))


(delay
  (-> data.generate-dataset/commit-dates
      (tc/select-rows #(:date %))
      (tc/group-by [:language])
      (tc/aggregate {:n-repos (fn [ds]
                                (-> ds
                                    :url
                                    distinct
                                    count))})))


(def date-counts
  (-> data.generate-dataset/commit-dates
      (tc/select-rows (->> data.generate-dataset/commit-dates
                           :date
                           ((partial datetime/long-temporal-field :years))
                           (map #(>= % 2011))))
      (tc/map-columns :group [:url] (fn [url]
                                      (str "G"
                                           (if (-> url
                                                   hash
                                                   even?)
                                             0 1))))
      (tc/group-by [:date :language :group])
      (tc/aggregate {:n tc/row-count})
      (tc/order-by [:date :language :group])))

(delay
  (-> date-counts
      (tc/select-rows #(-> % :language (= "Clojure")))
      (vis/hanami-plot ht/line-chart
                       {:X "date"
                        :XTYPE "temporal"
                        :Y "n"
                        :COLOR "group"
                        :OPACITY 0.4})))

(delay
  (-> date-counts
      (tc/group-by :language {:result-type :as-map})
      (->> (mapv (fn [[language counts]]
                   (-> counts
                       (vis/hanami-plot ht/line-chart
                                        {:X "date"
                                         :XTYPE "temporal"
                                         :Y "n"
                                         :COLOR "group"
                                         :OPACITY 0.4})))))))

(defn add-temporal-field [ds tf]
  (-> ds
      (tc/add-column tf (fn [ds]
                          (->> ds
                               :date
                               (datetime/long-temporal-field tf))))))

(defn add-smoothed-counts [ds window-size]
  (-> ds
      (tc/add-column (keyword (str "smoothed" window-size))
                     (fn [ds]
                       (-> ds
                           :n
                           (rolling/fixed-rolling-window
                            window-size
                            fun/mean
                            {:relative-window-position :left}))))))

(def processed-date-counts
  (-> date-counts
      (add-temporal-field :day-of-week)
      (add-temporal-field :day-of-year)
      (add-temporal-field :years)
      (tc/group-by [:language])
      (add-smoothed-counts 30)
      tc/ungroup))

(delay
  (-> processed-date-counts
      :day-of-year
      fun/reduce-max))

(delay
  (-> processed-date-counts
      (tc/group-by [:language :group] {:result-type :as-map})
      (->> (mapv (fn [[language counts]]
                   (-> counts
                       (tc/select-rows (fn [row]
                                         (-> row :years (#{2014 2018 2022}))))
                       (vis/hanami-plot ht/line-chart
                                        {:X "day-of-year"
                                         :Y "n"
                                         :COLOR "years"
                                         :OPACITY 0.5})))))))


(delay
  (-> processed-date-counts
      (tc/group-by [:language :group] {:result-type :as-map})
      (->> (mapv (fn [[lg counts]]
                   [lg (-> counts
                           (tc/select-rows (fn [row]
                                             (-> row :years (#{2014 2018 2022}))))
                           (vis/hanami-plot ht/line-chart
                                            {:X "day-of-year"
                                             :Y "smoothed30"
                                             :COLOR "years"
                                             :OPACITY 0.5}))])))))



(comment
  (defn plot-average-by-temporal-field [tf template]
    (-> date-counts
        (tc/add-column tf (fn [ds]
                            (->> ds
                                 :date
                                 (datetime/long-temporal-field tf))))
        (tc/group-by [tf])
        (tc/aggregate {:n (fn [ds]
                            (-> ds
                                :n
                                fun/mean))})
        (tc/order-by [tf])
        (vis/hanami-plot template
                         {:X tf
                          :Y "n"})))

  (plot-average-by-temporal-field :day-of-week
                                  ht/bar-chart)

(plot-average-by-temporal-field :day-of-year
                                ht/line-chart)


(-> processed-date-counts
    (vis/hanami-layers {}
                       [(vis/hanami-plot nil
                                         ht/line-chart
                                         {:X "date"
                                          :XTYPE "temporal"
                                          :Y "n"})
                        (vis/hanami-plot nil
                                         ht/line-chart
                                         {:X "date"
                                          :XTYPE "temporal"
                                          :Y "n-smoothed30"
                                          :MCOLOR "brown"})]))

(-> processed-date-counts
    (tc/select-rows (fn [row]
                      (and (-> row :years (> 2012))
                           (-> row :years (rem 3) (= 0)))))
    (vis/hanami-plot ht/line-chart
                     {:X "day-of-year"
                      :Y "n-smoothed30"
                      :YSCALE {:zero false}
                      :COLOR "years"
                      :OPACITY 0.5})))
