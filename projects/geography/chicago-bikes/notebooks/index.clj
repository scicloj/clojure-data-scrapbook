(load-file "../../../header.edn")

;; # Chicago bikes - DRAFT

;; This page will be updated soon.

;; ## Data sources:
;; - [Cyclistic bike share - Kaggle](https://www.kaggle.com/datasets/evangower/cyclistic-bike-share)
;; - [Cyclistic bike share - 2023 update - Kaggle](https://www.kaggle.com/datasets/godofoutcasts/cyclistic-bike-share-2023)
;; - [Chicago neighborhoods geojson by @blackmad](https://github.com/blackmad/neighborhoods/blob/master/chicago.geojson)


;; ## Workflow

(ns index
  (:require [tablecloth.api :as tc]
            [tech.v3.datatype.datetime :as datetime]
            [clojure.string :as str]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [fastmath-clustering.core :as clustering]
            [scicloj.kindly.v4.kind :as kind]))

(defonce raw-trips
  (-> "data/kaggle-cyclistic/202304_divvy_tripdata.csv.gz"
      (tc/dataset {:parser-fn {"started_at" [:local-date-time "yyyy-MM-dd HH:mm:ss"]
                               "ended_at" [:local-date-time "yyyy-MM-dd HH:mm:ss"]}
                   :key-fn keyword})))

(def coord-colnames
  [:start_lat :start_lng :end_lat :end_lng])


(def processed-trips
  (-> raw-trips
      (tc/add-column :hour (fn [trips]
                             (->> trips
                                  :started_at
                                  (datetime/long-temporal-field :hours))))
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

;; Let us plot a few clusters:

(defn as-png [vega-lite-spec]
  ;; to improve performance, we avoid rendering as SVG in this case:
  (-> vega-lite-spec
      (assoc :usermeta {:embedOptions {:renderer :png}})))

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
                     :map (fn [trips]
                            [(-> trips
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
                                                                      :OPACITY 0.01}))]})
                                 as-png)])
                     :hours (fn [trips]
                              [(hour-counts-plot trips)])})
      kind/table))
