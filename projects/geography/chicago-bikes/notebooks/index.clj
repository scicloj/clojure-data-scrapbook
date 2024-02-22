(ns index
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]))



(def trips
  ;; data source https://www.kaggle.com/datasets/evangower/cyclistic-bike-share
  (-> "data/202203-divvy-tripdata.csv.gz"
      (tc/dataset {:key-fn keyword})))

(-> trips
    (tc/group-by [:rideable_type])
    (tc/aggregate {:n tc/row-count}))
