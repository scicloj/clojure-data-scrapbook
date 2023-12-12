(ns notebooks.main-talk
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis :as vis]
            [tablecloth.api.columns :as tcc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [data.generate-dataset]
            [charred.api :as charred]
            [clojure.edn :as edn]))

;; # London Clojurians Talk December 2023

;; ## How do you work with data in Clojure?

;; What's in this dataset?

(-> "data/1000-repos.edn"
    slurp
    clojure.edn/read-string
    first)

;; Load data as a dataset

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

(-> clojure-repos
    (tc/map-columns :owner_handle [:owner] :login)
    (tc/select-columns [:full_name
                        :owner_handle
                        :watchers_count
                        :stargazers_count
                        :created_at]))

;; Do newer repos have fewer watchers?

(hanami/plot clojure-repos
             ht/point-chart
             {:X :created_at
              :XTYPE "temporal"
              :Y :watchers_count})

;; Outliers make this hard to interpret, so change the y-scale to make the graph easier to interpret

;; One option is to remove them

(-> clojure-repos
    (tc/select-rows #(< (:watchers_count %) 10000))
    (vis/hanami-plot ht/point-chart
                     {:X      :created_at
                      :XTYPE  "temporal"
                      :Y      :watchers_count}))

;; This isn't necessarily easier, another option is to change the y scale from a linear one to
;; logarithmic one, which is useful when you have a really big range of values because
;; logarithmic scales compensate for order of magnitude differences
;; i.e. 1, 10, 100 are equally spaced on a logarithmic scale, compared to a linear scale where
;; 1, 2, 3 are equally spaced

;; TODO: add more things to hanami plot hover labels

(-> clojure-repos
    ;; (tc/select-rows #(< (:watchers_count %) 10000))
    (vis/hanami-plot ht/point-chart
                     {:X     :created_at
                      :XTYPE "temporal"
                      :Y     :watchers_count
                      :YSCALE {:type "log"}}))

;; What about stargazers?

(vis/hanami-plot clojure-repos
                 ht/point-chart
                 {:X      :created_at
                  :XTYPE  "temporal"
                  :Y      :stargazers_count
                  :YSCALE {:type "log"}})

;; Is there a relationship between the number of contributors and stargazers?

(-> clojure-repos
    (tc/select-columns [:full_name :contributors_url])
    (tc/head 15)
    (tc/map-columns :contributors_count
                   [:contributors_url]
                   (fn [url] (->> url slurp charred/read-json
                                  (filter #(> 10 (get % "contributions"))) count))))



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
