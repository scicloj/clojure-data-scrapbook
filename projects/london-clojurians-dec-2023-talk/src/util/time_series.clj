(ns util.time-series
  (:require [tablecloth.api :as tc]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]))


(defn add-temporal-field [ds tf]
  (-> ds
      (tc/add-column tf (fn [ds]
                          (->> ds
                               :date
                               (datetime/long-temporal-field tf))))))

(defn add-smoothed-counts [ds window-size]
  (-> ds
      (tc/add-column (keyword (str "smoothed" window-size))
                     (fn [ds]
                       (-> ds
                           :n
                           (rolling/fixed-rolling-window
                            window-size
                            fun/mean
                            {:relative-window-position :left}))))))

(defn add-past-smoothed-counts [ds window-size]
  (-> ds
      (tc/add-column (keyword (str "past-smoothed" window-size))
                     (fn [ds]
                       (-> ds
                           :n
                           (fun/shift 1)
                           (rolling/fixed-rolling-window
                            window-size
                            fun/mean
                            {:relative-window-position :left}))))))
