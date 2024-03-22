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

(ns index
  (:require [tablecloth.api :as tc]
            [clojure.string :as str]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [fastmath-clustering.core :as clustering]
            [scicloj.kindly.v4.kind :as kind]))


(defonce raw-trips
  (-> "data/kaggle-cyclistic/202304_divvy_tripdata.csv.gz"
      (tc/dataset {:key-fn keyword})))

(def coord-colnames
  [:start_lat :start_lng :end_lat :end_lng])

(def processed-trips
  (-> raw-trips
      (tc/map-columns :hour [:started_at]
                      (fn [s]
                        (-> s
                            (str/split #" ")
                            second
                            (str/split #":")
                            first)))
      (tc/select-rows (fn [row]
                        (->> coord-colnames
                             (map row)
                             (every? some?))))))

(delay
  (-> processed-trips
      (tc/group-by [:hour])
      (tc/aggregate {:n tc/row-count})
      (tc/order-by [:hour])))


(defn hour-counts-plot [trips]
  (-> trips
      (tc/group-by [:hour])
      (tc/aggregate {:n tc/row-count})
      (hanami/plot ht/bar-chart
                   {:X :hour
                    :Y :n})))

(delay
  (hour-counts-plot processed-trips))


(defn as-geo [vega-lite-spec]
  (-> vega-lite-spec
      (update :encoding update-keys (fn [k]
                                      (get {:x :latitude
                                            :y :longitude
                                            :x2 :latitude2
                                            :y2 :longitude2}
                                           k k)))
      (assoc :projection {:type :mercator})))


(delay
  (-> processed-trips
      (tc/random 1000 {:seed 1})
      (hanami/plot ht/rule-chart
                   {:X :start_lat
                    :Y :start_lng
                    :X2 :end_lat
                    :Y2 :end_lng})
      as-geo))


(delay
  (-> processed-trips
      (tc/random 1000 {:seed 1})
      (hanami/plot ht/layer-chart
                   {:TITLE "Chicago bike trips"
                    :LAYER [{:data {:url "notebooks/data/chicago.geojson"
                                    :format {:type "topojson"}}
                             :mark {:type "geoshape"
                                    :filled false
                                    :clip true
                                    :opacity 0.3}}
                            (as-geo
                             (hanami/plot nil
                                          ht/rule-chart
                                          {:X :start_lat
                                           :Y :start_lng
                                           :X2 :end_lat
                                           :Y2 :end_lng
                                           :OPACITY 0.1}))]})))



(def clustering
  (-> processed-trips
      (tc/select-columns coord-colnames)
      tc/rows
      (clustering/k-means 100)
      (dissoc :data)))


(delay
  (-> processed-trips
      (tc/add-column :cluster (:clustering clustering))
      (tc/group-by [:cluster])
      (tc/without-grouping->
       (tc/order-by (fn [ds]
                      (-> ds :data tc/row-count))
                    :desc)
       (tc/head 5))
      (tc/aggregate {:n tc/row-count
                     :hours (fn [trips]
                              [(hour-counts-plot trips)])
                     :map (fn [trips]
                            [(-> trips
                                 (tc/random 1000 {:seed 1})
                                 (hanami/plot ht/layer-chart
                                              {:TITLE "Chicago bike trips"
                                               :LAYER [{:data {:url "notebooks/chicago.geojson"
                                                               :format {:type "topojson"}}
                                                        :mark {:type "geoshape"
                                                               :filled false
                                                               :clip true
                                                               :opacity 0.3}}
                                                       (as-geo
                                                        (hanami/plot nil
                                                                     ht/rule-chart
                                                                     {:X :start_lat
                                                                      :Y :start_lng
                                                                      :X2 :end_lat
                                                                      :Y2 :end_lng
                                                                      :OPACITY 0.01}))
                                                       (as-geo
                                                        (hanami/plot nil
                                                                     ht/point-chart
                                                                     {:X :start_lat
                                                                      :Y :start_lng
                                                                      :MCOLOR "purple"
                                                                      :OPACITY 0.01}))
                                                       (as-geo
                                                        (hanami/plot nil
                                                                     ht/point-chart
                                                                     {:X :end_lat
                                                                      :Y :end_lng
                                                                      :MCOLOR "green"
                                                                      :OPACITY 0.01}))]}))])})
      kind/table))
