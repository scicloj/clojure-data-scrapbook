(ns notebooks.contributors
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [scicloj.noj.v1.stats :as stats]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.dataset.print :as print]
            [tech.parallel :as parallel]
            [data.generate-dataset]
            [util.time-series :as time-series]
            [clojure.math :as math]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.set :as set]))



(def repos-growth
  (-> data.generate-dataset/commit-details
      (tc/group-by [:language :owner :date])
      (tc/aggregate {:n-commits tc/row-count
                     :n-emails #(-> % :email distinct count)
                     :emails #(-> % :email set vector)})
      (tc/rename-columns {:emails-0 :emails})
      (tc/group-by [:language :owner] {:result-type :as-map})
      (->> (filter (fn [[_ group-data]]
                     (-> group-data tc/row-count (>= 100))))
           (parallel/queued-pmap
            16
            (fn [[group group-data]]
              (-> group-data
                  (tc/order-by [:date])
                  (tc/add-columns {:acc-n-commits #(-> % :n-commits fun/cumsum)
                                   :acc-n-emails #(->> % :emails (reductions set/union) (map count))}))))
           (apply tc/concat))
      (tc/set-dataset-name "")
      time))


(->> repos-growth
     :emails
     (take 3))

(-> repos-growth
    (tc/group-by [:language :owner])
    (tc/aggregate
     (fn [ds]
       {:plot (-> ds
                  (hanami/combined-plot
                   ht/layer-chart
                   {:X "date"
                    :XTYPE "temporal"}
                   :LAYER [#_[ht/line-chart
                              {:Y "acc-n-commits"}]
                           [ht/line-chart
                            {:Y "acc-n-emails"
                             :MCOLOR "brown"}]]))
        :emails (->> ds :emails (apply set/union))}))
    kind/table)
