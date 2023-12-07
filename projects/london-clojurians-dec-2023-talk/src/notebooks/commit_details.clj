(ns notebooks.commit-details
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis :as vis]
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
    (vis/hanami-plot ht/point-chart
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
    (vis/hanami-plot ht/point-chart
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
    (tc/group-by [:language] {:result-type :as-map})
    (update-vals
     (fn [ds]
       (-> ds
           (tc/add-columns {:log-stargazers #(-> %
                                                 :stargazers_count
                                                 fun/log)
                            :log-contributors #(-> %
                                                   :n-contributors
                                                   fun/log)})
           (stats/add-predictions :log-stargazers
                                  [:log-contributors]
                                  {:model-type :smile.regression/ordinary-least-square})
           (vis/hanami-layers {}
                              [(vis/hanami-plot nil
                                                ht/point-chart
                                                {:X :log-contributors
                                                 :Y :log-stargazers
                                                 :XSCALE {:zero false}
                                                 :YSCALE {:zero false
                                                          :domain [5 12]}})
                               (vis/hanami-plot nil
                                                ht/line-chart
                                                {:X :log-contributors
                                                 :Y :log-stargazers-prediction
                                                 :MCOLOR "#91DC47"
                                                 :XSCALE {:zero false}
                                                 :YSCALE {:zero false
                                                          :domain [5 12]}})])))))
