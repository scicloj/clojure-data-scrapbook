(ns notebooks.live-talk-practice
  (:require [clojure.edn :as edn]
            [tablecloth.api :as tc]
            [aerial.hanami.templates :as ht]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [data.generate-dataset :as dataset]
            [tech.v3.datatype.functional :as fun]
            [scicloj.noj.v1.stats :as stats]))

;; # London Clojurians Dec 12 2023

;; What's in this dataset?

(-> "data/1000-repos.edn"
    slurp
    edn/read-string
    first)

(def clojure-repos
  (-> "data/1000-repos.edn"
      tc/dataset))

(-> clojure-repos
    tc/column-names
    sort)

(-> clojure-repos
    (tc/select-columns [:full_name :owner :watchers_count :created_at]))

;; Make new columns derived from existing ones

(-> clojure-repos
    (tc/map-columns :owner_handle [:owner] :login)
    (tc/select-columns [:full_name :owner_handle :watchers_count :created_at]))

;; Group and aggregate columns

(-> clojure-repos
    (tc/map-columns :owner_handle [:owner] :login)
    (tc/select-columns [:full_name :owner_handle :watchers_count :created_at])
    (tc/group-by [:owner_handle])
    (tc/as-regular-dataset)
    (tc/select-rows 0)
    :data)

(-> clojure-repos
    (tc/map-columns :owner_handle [:owner] :login)
    (tc/select-columns [:full_name :owner_handle :watchers_count :created_at])
    (tc/group-by [:owner_handle])
    (tc/aggregate {:repos_count tc/row-count})
    (tc/order-by :repos_count [:desc]))

(-> clojure-repos
    (tc/map-columns :owner_handle [:owner] :login)
    (tc/select-columns [:full_name :owner_handle :watchers_count :created_at])
    (tc/group-by [:owner_handle])
    (tc/aggregate {:total_watchers (fn [ds]
                                     (reduce + (:watchers_count ds)))}))

;; Do newer repos have fewer watchers?

(-> clojure-repos
    (hanami/plot ht/point-chart
                 {:X :created_at
                  :XTYPE "temporal"
                  :Y :watchers_count}
                 ))

;; Outliers make this hard to interpret

;; One option is to remove them

(-> clojure-repos
    (tc/select-rows #(< (:watchers_count %) 10000))
    (hanami/plot ht/point-chart
                 {:X     :created_at
                  :XTYPE "temporal"
                  :Y     :watchers_count}
                 ))

;; This isn't necessarily easier, another option is to change the y scale from a linear one to
;; logarithmic one, which is useful when you have a really big range of values because
;; logarithmic scales compensate for order of magnitude differences
;; i.e. 1, 10, 100 are equally spaced on a logarithmic scale, compared to a linear scale where
;; 1, 2, 3 are equally spaced

(-> clojure-repos
    ;; (tc/select-rows #(< (:watchers_count %) 10000))
    (hanami/plot ht/point-chart
                 {:X     :created_at
                  :XTYPE "temporal"
                  :Y     :watchers_count
                  :YSCALE {:type "log"}}))


;; Do newer repos have fewer stargazers?

(-> clojure-repos
    (hanami/plot ht/point-chart
                 {:X       :created_at
                  :XTYPE   "temporal"
                  :Y       :stargazers_count
                  :YSCALE  {:type "log"}
                  :MSIZE   60
                  :TOOLTIP [{:field "full_name"}
                            {:field "stargazers_count"}]}))


;; Is there a relationship between the number of contributors and stargazers?

(-> clojure-repos
    (tc/select-columns [:contributors_url]))

(def commit-details
  (tc/dataset "data/commit-details-2023-12-07T20:57:26.289-00:00.csv.gz"
              {:key-fn keyword}))

(tc/info commit-details)

(tc/head commit-details)

(-> commit-details
    (tc/group-by [:language :html_url])
    ;; count of unique email addresses per repo (treating html_url as repo id)
    (tc/aggregate {:contributors_count (fn [ds]
                                         (-> ds :email distinct count))})
    (tc/order-by [:contributors_count] :desc))

(-> commit-details
    (tc/group-by [:language :html_url])
    ;; count of unique email addresses per repo (treating html_url as repo id)
    (tc/aggregate {:contributors_count (fn [ds]
                                         (-> ds :email distinct count))})
    (tc/order-by [:contributors_count] :desc)
    (tc/left-join dataset/repos-ds [:html_url])
    (tc/group-by [:language])
    ;; (tc/drop-rows #(-> % :stargazers_count zero?))
    (hanami/plot ht/point-chart
                 {:X :contributors_count
                  :XSCALE {:type "log"}
                  :Y :stargazers_count
                  :YSCALE {:type "log"}})
    )

;; Note: on an M1 mac you have run an x86 JVM

(-> commit-details
    (tc/group-by [:language :html_url])
    ;; count of unique email addresses per repo (treating html_url as repo id)
    (tc/aggregate {:contributors_count (fn [ds]
                                         (-> ds :email distinct count))})
    (tc/order-by [:contributors_count] :desc)
    (tc/left-join dataset/repos-ds [:html_url])
    (tc/group-by [:language])
    (tc/add-columns {:log-stargazers #(-> % :stargazers_count fun/log)
                     :log-contributors #(-> % :contributors_count fun/log)})
    (stats/add-predictions :log-stargazers
                           [:log-contributors]
                           {:model-type :smile.regression/ordinary-least-square})
    (hanami/linear-regression-plot
      :log-contributors
      :log-stargazers
      {:XSCALE {:zero false}
       :YSCALE {:zero false}
       :line-options {:MCOLOR "brown"}
       :point-options {:MSIZE 70}})
    (tc/map-columns :plot [:plot]
                    #(assoc-in %
                               [:encoding :tooltip]
                               {:field "html_url"}))
    )
