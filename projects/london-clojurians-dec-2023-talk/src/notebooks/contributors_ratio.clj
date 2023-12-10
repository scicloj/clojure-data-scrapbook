(ns notebooks.contributors-ratio
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
            [clojure.set :as set]))

(defn spy [x tag]
  (prn [tag x])
  x)

(def repos-growth
  (-> data.generate-dataset/commit-details
      (tc/group-by [:language :html_url :date])
      (tc/aggregate {:n-commits tc/row-count
                     :n-emails #(-> % :email distinct count)
                     :commit-emails #(-> % :email vector)})
      (tc/rename-columns {:commit-emails-0 :commit-emails})
      (tc/group-by [:language :html_url] {:result-type :as-map})
      (->> (filter (fn [[_ group-data]]
                     (-> group-data tc/row-count (>= 100))))
           (parallel/queued-pmap
            16
            (fn [[group group-data]]
              (-> group-data
                  (tc/order-by [:date])
                  (tc/add-columns {;;
                                   :acc-n-commits
                                   (fn [ds] (-> ds
                                                :n-commits
                                                fun/cumsum))
                                   ;;
                                   :acc-n-commits-by-first
                                   (fn [ds]
                                     (let [most-frequent-email (->> ds
                                                                    :commit-emails
                                                                    (apply concat)
                                                                    frequencies
                                                                    (apply max-key val)
                                                                    key)]
                                       (prn [:most most-frequent-email])
                                       (-> (->> ds
                                                :commit-emails
                                                (map (fn [emails]
                                                       (->> emails
                                                            (filter (partial = most-frequent-email))
                                                            count)))
                                                (reductions +)))))}))))
           (apply tc/concat))
      (tc/set-dataset-name "")
      time))


(-> repos-growth
    (tc/group-by [:language :html_url])
    (hanami/combined-plot
     ht/layer-chart
     {:X "date"
      :XTYPE "temporal"
      :MSIZE 10
      :OPACITY 0.5}
     :LAYER [[ht/line-chart
              {:Y "acc-n-commits"}]
             [ht/line-chart
              {:Y "acc-n-commits-by-first"
               :MCOLOR "brown"}]]))


(-> repos-growth
    (tc/select-rows #(-> % :html_url (= "https://github.com/twosigma/Cook"))))
