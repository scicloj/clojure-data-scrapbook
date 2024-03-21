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
            [scicloj.kindly.v4.kind :as kind]))


(defonce trips
  (->> ["202104" "202105" "202106" "202107"
        "202108" "202109" "202110" "202111"
        "202112" "202201" "202202" "202203"]
       (map #(tc/dataset
              (format
               "data/kaggle-cyclistic/%s-divvy-tripdata.csv.gz" %)
              {:key-fn keyword}))
       (apply tc/concat)))

(delay
  (-> trips
      (tc/group-by [:start_station_name :end_station_name])
      (tc/aggregate {:n tc/row-count})
      (tc/order-by [:n] :desc)))

(def coord-column-names [:start_lat :start_lng
                         :end_lat :end_lng])



(defn draw-clusters [trips n-clusters]
  (let [clustering (-> trips
                       (tc/select-columns coord-column-names)
                       tc/rows
                       (->> (filter (fn [xs] (every? some? xs))))
                       (clustering/k-means n-clusters))
        clusters (-> clustering
                     :representatives
                     tc/dataset
                     (tc/rename-columns coord-column-names)
                     (tc/add-column :size (:sizes clustering))
                     (tc/add-column :i (fn [ds]
                                         (-> ds tc/row-count range))))]
    (kind/vega-lite
     (hc/xform ht/layer-chart
               :LAYER [{:data {:url "notebooks/data/chicago.geojson"
                               :format {:type "topojson"}}
                        :mark {:type "geoshape"
                               :filled false
                               :opacity 0.4}}
                       (-> clusters
                           (tc/order-by [:size])
                           (hanami/plot (assoc ht/view-base :mark "rule")
                                        {:X :start_lat :Y :start_lng
                                         :X2 :end_lat :Y2 :end_lng
                                         :SIZE 5
                                         :OPACITY 0.7
                                         :COLOR "size"
                                         :XSCALE {:zero false}
                                         :YSCALE {:zero false}
                                         :TITLE "Chicago bike trip clusters"})
                           (assoc-in [:encoding :color]
                                     {:field "size"
                                      :type "quantitative"})
                           (assoc :mark {:type "rule"
                                         :strokeCap "round"
                                         :strokeWidth 5}))
                       (-> clusters
                           (tc/order-by [:size])
                           (hanami/plot ht/point-chart
                                        {:X :start_lat :Y :start_lng
                                         :SIZE 100
                                         :XSCALE {:zero false}
                                         :YSCALE {:zero false}}))]))))



(-> trips
    (tc/map-columns :yearmonth [:started_at]
                    (fn [s]
                      (-> s
                          (subs 0 7))))
    (tc/select-rows #(-> % :yearmonth (= "2021-09")))
    (draw-clusters 50))



(-> trips
    (tc/group-by [:rideable_type] {:result-type :as-map})
    (update-vals #(draw-clusters % 200)))



(-> trips
    (tc/map-columns :hour [:started_at]
                    (fn [s]
                      (-> s
                          (str/split #" ")
                          second
                          (str/split #":")
                          first
                          Integer/parseInt)))
    (tc/map-columns :daypart [:hour]
                    (fn [h]
                      (quot h 8)))
    (tc/group-by [:daypart])
    (tc/aggregate (fn [ds]
                    [(draw-clusters ds 50)]))
    (tc/order-by [:daypart])
    kind/table)


(-> trips
    (tc/map-columns :yearmonth [:started_at]
                    (fn [s]
                      (-> s
                          (subs 0 7))))
    (tc/group-by [:yearmonth])
    (tc/aggregate (fn [ds]
                    (prn (-> ds :yearmonth first))
                    [(draw-clusters ds 500)]))
    (tc/order-by [:yearmonth])
    kind/table)


(-> trips
    (tc/group-by [:rideable_type])
    (tc/aggregate (fn [ds]
                    [(draw-clusters ds 100)]))
    kind/table)
