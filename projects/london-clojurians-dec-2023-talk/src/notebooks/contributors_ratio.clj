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

(defn add-freqs
  ([]
   {})
  ([freqs]
   freqs)
  ([freqs1 freqs2]
   (->> (concat freqs1 freqs2)
        (group-by key)
        (map (fn [[k freqs]]
               [k (->> freqs
                       (map val)
                       (reduce +))]))
        (into {}))))

(def repos-growth
  (-> data.generate-dataset/commit-details
      (tc/group-by [:language :html_url :date])
      (tc/aggregate {:n-commits tc/row-count
                     :n-committers #(-> % :email distinct count)
                     :committers #(-> % :email vector)})
      (tc/rename-columns {:committers-0 :committers})
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
                                   :acc-n-commits-by-frequent
                                   (fn [ds]
                                     (let [most-frequent-email-ranks (->> ds
                                                                          :committers
                                                                          (apply concat)
                                                                          frequencies
                                                                          (sort-by val)
                                                                          reverse
                                                                          (take 5)
                                                                          (map-indexed (fn [i [email times]]
                                                                                         [email i]))
                                                                          (into {}))]
                                       #_(prn [:most most-frequent-email-ranks])
                                       (-> (->> ds
                                                :committers
                                                (map (fn [emails]
                                                       (->> emails
                                                            (map #(-> %
                                                                      most-frequent-email-ranks
                                                                      (or "other")))
                                                            frequencies)))
                                                (reductions add-freqs)
                                                (map vector)))))}))))
           (apply tc/concat))
      (tc/set-dataset-name "")
      (tc/add-columns (->> (range 5)
                           (cons "other")
                           (map (fn [i]
                                  [(keyword (str "rank-" i))
                                   (fn [ds]
                                     (->> ds
                                          :acc-n-commits-by-frequent
                                          (map #(-> % first (get i 0)))))]))
                           (into {})))
      time))




(-> repos-growth
    (tc/select-columns [:language :html_url :date
                        :rank-0 :rank-1 :rank-2 :rank-3 :rank-4 :rank-other
                        ])
    (tc/pivot->longer (complement #{:language :html_url :date})
                      {:target-columns :committer
                       :value-column-name :acc-commits})
    (tc/group-by [:language :html_url])
    (hanami/combined-plot
     ht/layer-chart
     {:X "date"
      :XTYPE "temporal"
      :MSIZE 10}
     :LAYER [[ht/area-chart
              {:Y "acc-commits"
               :YSCALE {:type "log"}
               :COLOR "committer"}]]))


(-> repos-growth
    (tc/select-rows #(-> % :html_url (= "https://github.com/twosigma/Cook"))))
