(ns explore
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [scicloj.kindly.v4.api :as kindly]))

(defonce prices
  (-> "data/sp500_stocks.csv.gz"
      (tc/dataset {:key-fn keyword})
      (tc/rename-columns {(keyword "Adj Close") :Adj-Close})))


(map? prices)

(keys prices)

(-> prices
    :Symbol)

(-> prices
    :Symbol
    distinct)

(-> prices
    :Symbol
    distinct
    count)

(kindly/check = 503)

(-> prices
    (tc/group-by [:Symbol]))

(-> prices
    (tc/group-by [:Symbol])
    (tc/aggregate {:n tc/row-count})
    (tc/order-by [:n]))

(-> prices
    (tc/group-by [:symbol])
    (tc/aggregate {:n tc/row-count})
    :n
    distinct)



(-> prices
    (tc/select-rows (fn [{:keys [Symbol]}]
                      (= Symbol "ADBE"))))




(-> prices
    (tc/select-rows (fn [{:keys [Symbol]}]
                      (= Symbol "ADBE")))
    (tc/select-columns [:Date :Adj-Close])
    (hanami/plot ht/line-chart
                 {:X :Date
                  :XTYPE :temporal
                  :Y :Adj-Close}))
