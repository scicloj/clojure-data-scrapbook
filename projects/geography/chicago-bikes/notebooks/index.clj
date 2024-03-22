(ns index
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [aerial.hanami.common :as hc]
            [clojure.string :as str]
            [tech.v3.dataset :as tmd]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [fastmath-clustering.core :as clustering]
            [scicloj.kindly.v4.kind :as kind]
            [charred.api :as charred]))

(defonce trips
  (->> ["202203"]
       (map #(tc/dataset
              (format
               "data/kaggle-cyclistic/%s-divvy-tripdata.csv.gz" %)
              {:key-fn keyword}))
       (apply tc/concat)))

(-> "notebooks/data/chicago.geojson"
    slurp
    (charred/read-json {:key-fn keyword}))

(-> "notebooks/data/chicago.geojson"
    slurp
    (charred/read-json {:key-fn keyword})
    :features
    (->> (map (comp :coordinates :geometry))
         flatten
         (group-by pos?))
    (update-vals (fn [xs]
                   [(apply min xs)
                    (apply max xs)])))

(def corners
  (tc/dataset
   [#_{:start_lng -87.940114 :start_lat 41.644543 :end_lng -87.524137 :end_lat 42.023039}
    {:start_lng -87.7 :start_lat 41.7 :end_lng -87.6 :end_lat 41.9}]))


(def coord-column-names [:start_lat :start_lng
                         :end_lat :end_lng])

(defn as-geo [vega-lite-spec]
  (-> vega-lite-spec
      (update :encoding update-keys (fn [k]
                                      (get {:x :latitude
                                            :y :longitude
                                            :x2 :latitude2
                                            :y2 :longitude2}
                                           k k)))
      (assoc :projection {:type :mercator})))

(-> trips
    (tc/random 1000 {:seed 1})
    (tc/select-columns coord-column-names)
    (hanami/layers
     {:TITLE "Chicago bike trips"}
     [{:data {:url "notebooks/data/chicago.geojson"
              :format {:type "topojson"}}
       :mark {:type "geoshape"
              :filled false
              :opacity 0.4}}
      (as-geo (hanami/plot nil
                           (assoc ht/view-base :mark "rule")
                           {:X :start_lat :Y :start_lng
                            :X2 :end_lat :Y2 :end_lng
                            :OPACITY 0.5}))
      (as-geo (hanami/plot corners
                           (assoc ht/view-base :mark "rule")
                           {:X :start_lat :Y :start_lng
                            :X2 :end_lat :Y2 :end_lng
                            :OPACITY 1
                            :SIZE 0}))]))

(def trips-with-coords
  (-> trips
      (tc/select-rows (fn [row]
                        (->> coord-column-names
                             (map row)
                             (every? some?))))))

(let [clustering (-> trips-with-coords
                     (tc/select-columns coord-column-names)
                     tc/rows
                     (clustering/k-means 100))]
  (-> trips-with-coords
      (tc/add-column :clustering (:clustering clustering))
      (tc/group-by [:clustering])
      (tc/aggregate {:n tc/row-count
                     :plot (fn [ds]
                             [(-> ds
                                  (tc/select-columns coord-column-names)
                                  (hanami/layers
                                   {:TITLE "Chicago bike trip clusters"}
                                   [(as-geo
                                     (hanami/plot nil
                                                  (assoc ht/view-base :mark "rule")
                                                  {:X :start_lat :Y :start_lng
                                                   :X2 :end_lat :Y2 :end_lng
                                                   :OPACITY 0.01
                                                   :XSCALE {:zero false}
                                                   :YSCALE {:zero false}}))
                                    (as-geo
                                     (hc/xform
                                      ht/point-layer
                                      {:X :start_lat :Y :start_lng
                                       :OPACITY 0.01
                                       :MCOLOR "purple"}))
                                    (as-geo
                                     (hc/xform
                                      ht/point-layer
                                      {:X :end_lat :Y :end_lng
                                       :OPACITY 0.01
                                       :MCOLOR "green"}))
                                    (as-geo (hanami/plot
                                             corners
                                             (assoc ht/view-base :mark "rule")
                                             {:X :start_lat :Y :start_lng
                                              :X2 :end_lat :Y2 :end_lng
                                              :OPACITY 0}))
                                    {:data {:url "notebooks/data/chicago.geojson"
                                            :format {:type "topojson"}}
                                     :mark {:type "geoshape"
                                            :filled false
                                            :clip true
                                            :opacity 0.4}}])
                                  (assoc :height 2000
                                         :width 500))])})
      (tc/order-by [:n] :desc)
      (tc/head 10)
      kind/table))
