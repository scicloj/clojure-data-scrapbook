(ns notebooks.other-contributors
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
            [clojure.set :as set])
  (:import java.time.temporal.ChronoUnit
           java.time.LocalDate))

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

(defonce repos-growth
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
                                   :all-commits
                                   (fn [ds] (-> ds
                                                :n-commits
                                                fun/cumsum))
                                   ;;
                                   :by-frequent
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
                                          :by-frequent
                                          (map #(-> % first (get i 0)))))]))
                           (into {})))
      time))

(def lifespans
  (-> repos-growth
      (tc/group-by [:language :html_url])
      (tc/aggregate {:lifespan (fn [ds]
                                 (-> ds
                                     :date
                                     (#(.until ^LocalDate (first %)
                                               ^LocalDate (last %)
                                               ChronoUnit/DAYS))))})))


(delay
  (-> repos-growth
      (tc/group-by [:language :html_url])
      (hanami/plot
       ht/line-chart
       {:X "date"
        :XTYPE "temporal"
        :MSIZE 10
        :Y "all-commits"})))




(def rank-columns
  [:rank-0 :rank-1 :rank-2 :rank-3 :rank-4 :rank-other])

(delay
  (-> repos-growth
      (tc/select-columns (concat [:language :html_url :date :all-commits]
                                 rank-columns))
      (tc/pivot->longer (complement #{:language :html_url :date :all-commits})
                        {:target-columns :committer
                         :value-column-name :commits})
      (tc/group-by [:language :html_url])
      (hanami/plot
       ht/area-chart
       {:X "date"
        :XTYPE "temporal"
        :MSIZE 10
        :Y "commits"
        :COLOR "committer"})))


(delay
  (-> repos-growth
      (tc/select-columns (concat [:language :html_url :date :all-commits]
                                 rank-columns))
      (tc/pivot->longer (complement #{:language :html_url :date})
                        {:target-columns :committer
                         :value-column-name :commits})
      (tc/group-by [:language :html_url])
      (tc/aggregate {:total-commits (fn [ds]
                                      (-> ds
                                          (tc/select-rows #(-> % :committer (= :all-commits)))
                                          :commits
                                          last))
                     :total-other-commits (fn [ds]
                                            (-> ds
                                                (tc/select-rows #(-> % :committer (= :rank-other)))
                                                :commits
                                                last))
                     :plot (fn [ds]
                             (-> ds
                                 (tc/drop-rows #(-> % :committer (= :all-commits)))
                                 (hanami/plot
                                  ht/area-chart
                                  {:X "date"
                                   :XTYPE "temporal"
                                   :MSIZE 10
                                   :Y "commits"
                                   :COLOR "committer"})
                                 vector))})
      (tc/add-column :other-ratio (fn [ds]
                                    (fun// (:total-other-commits ds)
                                           (:total-commits ds))))
      (tc/order-by [:other-ratio] :desc)
      kind/table))

(delay
  (-> repos-growth
      (tc/select-columns (concat [:language :html_url :date :all-commits]
                                 rank-columns))
      (tc/pivot->longer (complement #{:language :html_url :date})
                        {:target-columns :committer
                         :value-column-name :commits})
      (tc/group-by [:language :html_url])
      (tc/aggregate {:total-commits (fn [ds]
                                      (-> ds
                                          (tc/select-rows #(-> % :committer (= :all-commits)))
                                          :commits
                                          last))
                     :total-other-commits (fn [ds]
                                            (-> ds
                                                (tc/select-rows #(-> % :committer (= :rank-other)))
                                                :commits
                                                last))})
      (tc/select-rows #(-> % :total-other-commits pos?))
      (hanami/plot ht/point-chart
                   {:X :total-commits
                    :Y :total-other-commits
                    :XSCALE {:type "log"}
                    :YSCALE {:type "log"}
                    :COLOR "language"
                    :MSIZE 50})))



(delay
  (-> repos-growth
      (tc/select-columns (concat [:language :html_url :date :all-commits]
                                 rank-columns))
      (tc/pivot->longer (complement #{:language :html_url :date})
                        {:target-columns :committer
                         :value-column-name :commits})
      (tc/group-by [:language :html_url])
      (tc/aggregate {:total-commits (fn [ds]
                                      (-> ds
                                          (tc/select-rows #(-> % :committer (= :all-commits)))
                                          :commits
                                          last))
                     :total-other-commits (fn [ds]
                                            (-> ds
                                                (tc/select-rows #(-> % :committer (= :rank-other)))
                                                :commits
                                                last))})
      (tc/select-rows #(-> % :total-other-commits pos?))
      (tc/add-column :other-ratio (fn [ds]
                                    (fun// (:total-other-commits ds)
                                           (:total-commits ds))))
      (tc/left-join lifespans [:language :html_url])
      (tc/drop-columns [:right.language :right.html_url])
      (tc/left-join data.generate-dataset/repos-ds [:language :html_url])
      (tc/drop-columns [:right.language :right.html_url])
      (tc/add-column :log-stargazers-count #(-> %
                                                :stargazers_count
                                                fun/log))
      (tc/add-column :log-total-other-commits #(-> %
                                                   :total-other-commits
                                                   (fun/+ 1)
                                                   fun/log))
      (tc/group-by [:language])
      (hanami/linear-regression-plot :log-total-other-commits
                                     :log-stargazers-count
                                     {:point-options {:MSIZE 100
                                                      :OPACITY 0.6
                                                      :XSCALE {:domain [4 12]}
                                                      :YSCALE {:domain [0 11]}}
                                      :line-options {:MCOLOR "brown"}})
      (tc/map-columns :plot [:plot]
                      #(assoc-in %
                                 [:encoding :tooltip]
                                 {:field "html_url"}))))
