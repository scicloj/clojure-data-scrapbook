(ns notebooks.contributors
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [fastmath.stats]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.dataset.print :as print]
            [tech.parallel :as parallel]
            [data.generate-dataset]
            [util.time-series :as time-series]
            [clojure.math :as math]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.set :as set]
            [loom.graph]
            [loom.alg]
            [loom.io]))



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


(-> repos-growth
    (tc/group-by [:language :owner])
    (hanami/plot
     ht/line-chart
     {:X "date"
      :XTYPE "temporal"
      :Y "acc-n-emails"
      :MCOLOR "brown"}))


(defn adapt-to-max-1 [xs]
  (fun// (dtype/->double-array xs)
         (fun/reduce-max xs)))

(def owner->normalized-contrib-times
  (-> repos-growth
      (tc/group-by :owner
                   {:result-type :as-map})
      (update-vals
       (fn [group-data]
         {:data group-data
          :times (->> group-data
                      :acc-n-emails
                      adapt-to-max-1)}))))

(def tested-pairs
  (->> (for [[owner1 info1] owner->normalized-contrib-times
             [owner2 info2] owner->normalized-contrib-times]
         {:owner1 owner1
          :owner2 owner2
          :times1 (:times info1)
          :times2 (:times info2)})
       (parallel/queued-pmap
        32
        (fn [{:as row
              :keys [times1 times2]}]
          (merge row
                 (fastmath.stats/ks-test-two-samples
                  times1 times2))))
       tc/dataset
       time))


(-> tested-pairs
    (hanami/histogram :KS {:nbins 100}))


(-> tested-pairs
    (hanami/plot ht/point-chart
                 {:X :KS
                  :Y :p-value}))

(def components
  (-> tested-pairs
      (tc/select-rows #(-> % :KS (< 0.1)))
      (tc/rows :as-maps)
      (->> (map (juxt :owner1 :owner2))
           (apply loom.graph/graph))
      loom.alg/connected-components))

(->> components
     (filter #(-> % count (> 1)))
     count)

(->> components
     (filter #(-> % count (> 1)))
     (map (fn [owners]
            [(kind/hiccup [:h1 (count owners) " owners"])
             (let [owners-set (set owners)]
               (-> repos-growth
                   (tc/select-rows #(-> % :owner owners-set))
                   (tc/group-by [:language :owner])
                   (hanami/plot
                    ht/line-chart
                    {:X "date"
                     :XTYPE "temporal"
                     :Y "acc-n-emails"
                     :MCOLOR "brown"})))])))
