(ns notebooks.commits
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis :as vis]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [data.generate-dataset]))

(def date-counts
  (-> data.generate-dataset/commit-dates
      (tc/group-by [:date])
      (tc/aggregate {:n tc/row-count})
      (tc/order-by [:date])))

date-counts

(-> date-counts
    (vis/hanami-plot ht/line-chart
                     {:X "date"
                      :XTYPE "temporal"
                      :Y "n"}))

(defn add-temporal-field [ds tf]
  (-> ds
      (tc/add-column tf (fn [ds]
                          (->> ds
                               :date
                               (datetime/long-temporal-field tf))))))

(def processed-date-counts
  (-> date-counts
      (add-temporal-field :day-of-week)
      (add-temporal-field :day-of-year)
      (add-temporal-field :years)))

(-> processed-date-counts
    :day-of-year
    fun/reduce-max)

(-> processed-date-counts
    (tc/select-rows (fn [row]
                      (-> row :years (#{2008 2012 2016 2020}))))
    (vis/hanami-plot ht/line-chart
                     {:X "day-of-year"
                      :Y "n"
                      :COLOR "years"
                      :OPACITY 0.5}))



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
    (tc/add-column :smoothed-n
                   (fn [ds]
                     (-> ds
                         :n
                         (rolling/fixed-rolling-window
                          30
                          fun/mean
                          {:relative-window-position :left}))))
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
                                          :Y "smoothed-n"
                                          :MCOLOR "brown"})]))
