(ns notebooks.model
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [scicloj.noj.v1.stats :as stats]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.dataset.print :as print]
            [data.generate-dataset]
            [util.time-series :as time-series]
            [clojure.math :as math]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.ml.core :as ml]
            [scicloj.ml.metamorph :as mm]
            [scicloj.ml.dataset :as ds]))

(defonce date-counts
  (-> data.generate-dataset/commit-dates
      (time-series/add-temporal-field :years)
      (tc/select-rows #(<= 2011 (:years %) 2023))
      (tc/group-by [:date :language :owner])
      (tc/aggregate {:n tc/row-count})
      (tc/order-by [:date :language :owner])))

(def just-dates
  (-> date-counts
      :date
      distinct
      (->> (hash-map :date))
      tc/dataset
      (tc/order-by [:date])))

(defn log1+ [xs]
  (-> xs
      (fun/+ 1)
      fun/log))

(defn exp-1 [ys]
  (-> ys
      fun/exp
      (fun/- 1)))

(defonce processed-counts
  (-> date-counts
      (tc/group-by [:language :owner]
                   {:result-type :as-map})
      (->> (map (fn [[group group-subset]]
                  (-> group-subset
                      (tc/right-join just-dates [:date])
                      (tc/map-columns :date [:right.date] identity)
                      (tc/add-columns {:language (:language group)
                                       :owner (:owner group)})
                      (tc/drop-columns [:right.date])
                      (tc/order-by [:date])
                      (tc/map-columns :n [:n]
                                      #(or % 0))
                      (time-series/add-past-smoothed-counts 30)
                      (time-series/add-temporal-field :day-of-week)
                      (tc/add-columns {:log1+n #(-> % :n log1+)
                                       :log1+past-smoothed30 #(-> % :past-smoothed30 log1+)}))))
           (apply tc/concat))
      time))

(def owner->sample
  (memoize
   (fn [owner]
     (-> owner
         hash
         even?
         ({true :train
           false :test})))))


(def split-data
  (-> processed-counts
      (tc/map-columns :sample [:owner] owner->sample)
      (tc/group-by :sample {:result-type :as-map})
      (update-vals (fn [ds]
                     (-> ds
                         (tc/order-by [:language :owner :date]))))))


(def pipe-fn
  (ml/pipeline
   (mm/set-inference-target :log1+n)
   (mm/select-columns [:language :day-of-week :log1+past-smoothed30 :log1+n])
   (mm/categorical->one-hot [:language :day-of-week])
   (mm/drop-columns [:day-of-week :language])
   {:metamorph/id :model}
   (mm/model {:model-type :smile.regression/elastic-net})))

(def trained-ctx
  (pipe-fn
   {:metamorph/data (:train split-data)
    :metamorph/mode :fit}))

(-> trained-ctx
    :model
    ml/explain)

(def predicted-ctx
  (pipe-fn
   (merge trained-ctx
          {:metamorph/data (:test split-data)
           :metamorph/mode :transform})))


(delay
  (-> {:predicted (-> predicted-ctx
                      :metamorph/data
                      :log1+n)
       :actual (-> split-data
                   :test
                   :log1+n)}
      tc/dataset
      ((juxt
        #(stats/calc-correlations-matrix
          % [:predicted :actual])
        #(hanami/plot % ht/point-chart
                      {:X :predicted
                       :Y :actual})))))


(def test-with-predictions
  (-> split-data
      :test
      (tc/add-column :predicted
                     (-> predicted-ctx
                         :metamorph/data
                         :log1+n))
      (tc/add-column :residual
                     #(fun/- (:log1+n %)
                             (:predicted %)))))

(-> test-with-predictions
    (tc/group-by [:language] {:result-type :as-map})
    (update-vals (fn [ds]
                   (-> ds
                       (stats/calc-correlations-matrix [;:log1+past-smoothed30
                                                        :log1+n
                                                        :predicted])))))

(-> test-with-predictions
    (tc/group-by [:language :owner])
    (tc/aggregate {:total-n (fn [ds]
                              (-> ds
                                  :log1+n
                                  exp-1
                                  fun/sum))})
    (hanami/histogram :total-n {:nbins 100}))



(def test-with-predictions-active-owners
  (-> test-with-predictions
      (tc/group-by [:language :owner] {:result-type :as-map})
      vals
      (->> (filter (fn [ds]
                     (-> ds
                         :log1+n
                         exp-1
                         fun/sum
                         (> 1000))))
           (apply tc/concat))
      (tc/set-dataset-name "")))

(-> test-with-predictions-active-owners
    (hanami/histogram :residual {:nbins 100}))


(-> test-with-predictions-active-owners
    (time-series/add-temporal-field :years)
    (tc/select-rows #(and (-> % :residual (> 2))
                          (-> % :years (>= 2019))))
    (hanami/plot ht/point-chart
                 {:X "date"
                  :XTYPE "temporal"
                  :Y :residual
                  :COLOR "language"}))
