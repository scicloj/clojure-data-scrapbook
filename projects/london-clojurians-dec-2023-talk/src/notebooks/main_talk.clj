(ns notebooks.main-talk
  (:require [tablecloth.api :as tc]
            [tablecloth.api.columns :as tcc]
            [scicloj.noj.v1.vis :as vis]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [data.generate-dataset]))

;; # London Clojurians Talk December 2023

;; ## Clean up dataset

(def clojure-repos
  (-> "data/1000-repos.edn"
      tc/dataset))

(def commit-dates
  (-> "data/commit-dates-2023-12-05T19:48:16.843-00:00.csv.gz"
      (tc/dataset {:key-fn keyword})))

;; Explore the dataset with tablecloth

;; Get column names

(-> clojure-repos
    tc/column-names
    sort)

;; Narrow it down to ones we care about

(tc/select-columns clojure-repos [:full_name :owner :watchers_count :created_at])

;; Owners are their whole own things.. too much information

;; inspect one
(-> clojure-repos (tc/select-rows 0) :owner)

(-> clojure-repos (tc/select-rows 0) :owner first :login)

(def smaller-clojure-repos
  (-> clojure-repos
      (tc/map-columns :owner_handle [:owner] :login)
      (tc/select-columns [:html_url
                          :full_name
                          :owner_handle
                          :watchers_count
                          :created_at])
      ))

(vis/hanami-plot clojure-repos
                 ht/point-chart
                 {:X :created_at
                  :XTYPE "temporal"
                  :Y :watchers_count})

;; Group repos by owner

(-> smaller-clojure-repos
    (tc/group-by [:owner_handle])
    (tc/aggregate {:repos_count tc/row-count})
    (tc/order-by :repos_count [:desc]))

(-> clojure-repos
    (tc/map-columns :owner_handle [:owner] :login)
    (tc/group-by [:owner_handle])
    (tc/aggregate {:total_watchers (fn [ds]
                                     (reduce + (:watchers_count ds)))}))

(-> commit-dates tc/column-names)

(-> commit-dates
    (tc/select-rows )
    (tc/group-by [:date])
    )

(def date-counts
  (-> commit-dates
      (tc/select-rows (->> commit-dates
                           :date
                           ((partial datetime/long-temporal-field :years))
                           (map #(>= % 2011))))
      (tc/group-by [:date :language])
      (tc/aggregate {:n tc/row-count})
      (tc/order-by [:date :language])))

;; - Extract relevant data from massive JSON blob
;; - Clean up data, explain/interpret columns


;; ## Basic Analysis

;; - Discover trends
;; - Projections


;; ## Visualization

;; - Plot/graph the above


;; ----------

;; ## To talk about

;; ## Daniel -- introduce workflows, explain where things are at
;; ## Kira -- examples in the context of the cookbook?
;; ## Timothy -- talk about some tooling developments, goals of clay/claykind/etc
