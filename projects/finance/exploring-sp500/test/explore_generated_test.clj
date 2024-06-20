(ns
 explore-generated-test
 (:require
  [tablecloth.api :as tc]
  [scicloj.noj.v1.vis.hanami :as hanami]
  [aerial.hanami.templates :as ht]
  [scicloj.kindly.v4.api :as kindly]
  [clojure.test :refer [deftest is]]))


(def
 var1
 (defonce
  prices
  (->
   "data/sp500_stocks.csv.gz"
   (tc/dataset {:key-fn keyword})
   (tc/rename-columns {(keyword "Adj Close") :Adj-Close}))))


(def var2 (map? prices))


(def var3 (keys prices))


(def var4 (-> prices :Symbol))


(def var5 (-> prices :Symbol distinct))


(def var6 (-> prices :Symbol distinct count))


(deftest test7 (is (= var6 503)))


(def var8 (-> prices (tc/group-by [:Symbol])))


(def
 var9
 (->
  prices
  (tc/group-by [:Symbol])
  (tc/aggregate {:n tc/row-count})
  (tc/order-by [:n])))


(def
 var10
 (->
  prices
  (tc/group-by [:symbol])
  (tc/aggregate {:n tc/row-count})
  :n
  distinct))


(def
 var11
 (-> prices (tc/select-rows (fn [{:keys [Symbol]}] (= Symbol "ADBE")))))


(def
 var12
 (->
  prices
  (tc/select-rows (fn [{:keys [Symbol]}] (= Symbol "ADBE")))
  (tc/select-columns [:Date :Adj-Close])
  (hanami/plot
   ht/line-chart
   {:X :Date, :XTYPE :temporal, :Y :Adj-Close})))
