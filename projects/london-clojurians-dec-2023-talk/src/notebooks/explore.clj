(ns notebooks.explore
  (:require [data.generate-dataset]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.noj.v1.vis :as vis]
            [aerial.hanami.templates :as ht]
            [tablecloth.api :as tc]
            [clojure.string :as string]
            [tech.v3.datatype.datetime :as datetime]))

(def processed-data
  (-> (->> data.generate-dataset/data
           (mapcat (comp :items :items))
           (map #(select-keys % [:created_at :html_url :open_issues_count :watchers :forks_count])))
      tc/dataset
      (tc/map-columns :user [:html_url]
                      (fn [html-url]
                        (-> html-url
                            (string/split #"/")
                            (->> (drop 3)
                                 first))))
      (tc/map-columns :clojure-user [:user]
                      (partial = "clojure"))
      (tc/map-columns :babashka-user [:user]
                      (partial = "babashka"))))

(-> processed-data
    (tc/group-by [:user])
    (tc/aggregate {:n tc/row-count})
    (tc/order-by [:n] :desc))

(-> processed-data
    (tc/select-rows #(and (-> % :forks_count (> 0))
                          (-> % :open_issues_count (> 0))))
    (vis/hanami-plot ht/point-chart
                     {:X "open_issues_count"
                      :Y "forks_count"
                      :XSCALE {:type "log"}
                      :YSCALE {:type "log"}
                      :COLOR "babashka-user"
                      :SIZE "watchers"})
    (assoc-in [:encoding :size :type] "quantitative")
    (assoc-in [:encoding :size :scale :type] "log"))


(->> data.generate-dataset/data
     (mapcat (comp :items :items))
     first)

(-> processed-data
    (vis/hanami-plot ht/bar-chart
                     {:X "created_at"
                      :XTYPE "temporal"
                      :Y "watchers"}))

(-> processed-data
    (tc/map-columns :yearmonth
                    [:created_at]
                    #(subs % 0 7)))

(-> processed-data
    (tc/map-columns :yearmonth
                    [:created_at]
                    #(subs % 0 7))
    (tc/group-by [:yearmonth])
    (tc/aggregate {:n tc/row-count})
    (tc/order-by [:yearmonth]))



(-> processed-data
    (tc/map-columns :yearmonth
                    [:created_at]
                    #(subs % 0 7))
    (tc/group-by [:yearmonth])
    (tc/aggregate {:n tc/row-count})
    (tc/order-by [:yearmonth])
    (vis/hanami-plot ht/bar-chart
                     {:X "yearmonth"
                      :XTYPE "temporal"
                      :Y "n"}))

(-> processed-data
    (tc/head 100)
    (tc/map-columns :yearmonth
                    [:created_at]
                    #(subs % 0 7))
    (tc/group-by [:yearmonth])
    (tc/aggregate {:n tc/row-count})
    (tc/order-by [:yearmonth])
    (vis/hanami-plot ht/bar-chart
                     {:X "yearmonth"
                      :XTYPE "temporal"
                      :Y "n"}))
