(ns notebooks.commit-details
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.dataset.print :as print]
            [data.generate-dataset]
            [clojure.math :as math]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.noj.v1.stats :as stats]))



(-> data.generate-dataset/commit-details
    (tc/group-by [:language :html_url])
    (tc/aggregate {:n-contributors (fn [ds]
                                     (-> ds
                                         :email
                                         distinct
                                         count))})
    (tc/order-by [:n-contributors] :desc))


(-> data.generate-dataset/commit-details
    (tc/select-rows #(-> % :html_url (= "https://github.com/technomancy/leiningen")))
    (tc/group-by [:email])
    (tc/aggregate {:n tc/row-count})
    (tc/order-by [:n] :desc))



(-> data.generate-dataset/commit-details
    (tc/group-by [:language :html_url])
    (tc/aggregate {:n-contributors (fn [ds]
                                     (-> ds
                                         :email
                                         distinct
                                         count))})
    (tc/order-by [:n-contributors] :desc)
    (tc/left-join data.generate-dataset/repos-ds [:html_url])
    (tc/select-rows #(-> % :n-contributors (> 20)))
    (hanami/plot ht/point-chart
                 {:X :n-contributors
                  :XSCALE {:type "log"}
                  :Y :stargazers_count
                  :YSCALE {:type "log"}
                  :COLOR "language"}))


(-> data.generate-dataset/commit-details
    (tc/group-by [:language :html_url])
    (tc/aggregate {:n-contributors (fn [ds]
                                     (-> ds
                                         :email
                                         distinct
                                         count))})
    (tc/order-by [:n-contributors] :desc)
    (tc/left-join data.generate-dataset/repos-ds [:html_url])
    (tc/select-rows #(-> % :n-contributors (> 20)))
    (tc/group-by [:language])
    (hanami/plot ht/point-chart
                 {:X :n-contributors
                  :XSCALE {:type "log"}
                  :Y :stargazers_count
                  :YSCALE {:type "log"}}))



(-> data.generate-dataset/commit-details
    (tc/group-by [:language :html_url])
    (tc/aggregate {:n-contributors (fn [ds]
                                     (-> ds
                                         :email
                                         distinct
                                         count))})
    (tc/order-by [:n-contributors] :desc)
    (tc/left-join data.generate-dataset/repos-ds [:html_url])
    (tc/select-rows #(-> % :n-contributors (> 20)))
    (tc/group-by [:language])
    (tc/add-columns {:log-stargazers #(-> %
                                          :stargazers_count
                                          fun/log)
                     :log-contributors #(-> %
                                            :n-contributors
                                            fun/log)})
    (stats/add-predictions :log-stargazers
                           [:log-contributors]
                           {:model-type :smile.regression/ordinary-least-square})
    (hanami/combined-plot
     ht/layer-chart
     {:X :log-contributors
      :XSCALE {:zero false}
      :YSCALE {:zero false
               :domain [5 12]}}
     :LAYER [[ht/point-chart
              {:Y :log-stargazers}]
             [ht/line-chart
              {:Y :log-stargazers-prediction
               :MCOLOR "brown"}]]))
