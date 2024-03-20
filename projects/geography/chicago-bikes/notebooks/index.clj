(ns index
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [clojure.string :as str]))

(defonce data
  (-> "data/kaggle-divvy-weather/data.csv.gz"
      (tc/dataset {:key-fn keyword})))

(delay
  (-> data
      (tc/group-by [:from_station_name :to_station_name])
      (tc/aggregate {:n tc/row-count})
      (tc/order-by [:n] :desc)
      tc/head))

(def data-of-most-popular-route
  (-> data
      (tc/select-rows (fn [row]
                        (and (-> row :from_station_name (= "Columbus Dr & Randolph St"))
                             (-> row :to_station_name (= "Clinton St & Washington Blvd")))))))


(-> data-of-most-popular-route
    (tc/map-columns :date
                    [:year :month :day]
                    (partial format "%s-%02d-%02d"))
    :date)


(-> data-of-most-popular-route
    (hanami/plot ht/point-chart
                 {:X "temperature"
                  :Y "tripduration"
                  :YSCALE {:type "log"}
                  :COLOR "events"}))
