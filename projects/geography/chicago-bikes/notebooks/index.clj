(load-file "../../../header.edn")

;; # Chicago bikes - DRAFT

;; This page will be updated soon.

;; ## Data sources:
;; - [Cyclistic bike share - Kaggle](https://www.kaggle.com/datasets/evangower/cyclistic-bike-share)
;; - [Cyclistic bike share - 2023 update - Kaggle](https://www.kaggle.com/datasets/godofoutcasts/cyclistic-bike-share-2023)
;; - [Chicago neighborhoods geojson by @blackmad](https://github.com/blackmad/neighborhoods/blob/master/chicago.geojson)


;; ## Setup

(ns index
  (:require [tablecloth.api :as tc]
            [clojure.math :as math]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.functional :as fun]
            [clojure.string :as str]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [fastmath-clustering.core :as clustering]
            [geo
             [geohash :as geohash]
             [jts :as jts]
             [spatial :as spatial]
             [io :as geoio]
             [crs :as crs]]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly])
  (:import (org.locationtech.jts.geom Geometry Point Polygon Coordinate)
           (org.locationtech.jts.geom.prep PreparedGeometry
                                           PreparedLineString
                                           PreparedPolygon
                                           PreparedGeometryFactory)))


^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

;; ## Reading the data

(defonce raw-trips
  (-> "data/kaggle-cyclistic/202304_divvy_tripdata.csv.gz"
      (tc/dataset {:parser-fn {"started_at" [:local-date-time "yyyy-MM-dd HH:mm:ss"]
                               "ended_at" [:local-date-time "yyyy-MM-dd HH:mm:ss"]}
                   :key-fn keyword})))

;; ## Coordinate conversions

(md "
The datasets uses the [WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System)
coordinate system, representing latitude and longitued over the globe.
[EPSG:4326](https://epsg.io/4326)")

(md "For metric computations (e.g., k-means clustering),
we need to convert them to a coordinate system
which is locally correct in terms of distances in a region around Chicago:
NAD83 / Illinois East
[EPSG:26971](https://epsg.io/26971)")

^:kindly/hide-code
(-> {"Center coordinates" [[316133.6 345590.74]]
     "Projected bounds"   [[[216692.35 43649.23]
                            [416809.98 648476.24]]]
     "WGS84 bounds"       [[[-89.27 37.06]
                            [-87.02 42.5]]]}
    tc/dataset
    (tc/set-dataset-name "United States (USA) - Oregon and Washington."))

(def crs-transform
  (geo.crs/create-transform (geo.crs/create-crs 4326)
                            (geo.crs/create-crs 26971)))

(defn wgs84->Chicago
  "Transforming latitude-longitude coordinates
  to local Euclidean coordinates around Seattle."
  [geometry]
  (geo.jts/transform-geom geometry crs-transform))

(defn lng-lat->local-coords [lng lat]
  (-> (geo.jts/coordinate lng lat)
      geo.jts/point
      wgs84->Chicago
      geo.jts/coord
      ((fn [^Coordinate coord]
         [(.getX coord)
          (.getY coord)]))))

;; Example:

(delay
  [(lng-lat->local-coords -89.27 37.06)
   (lng-lat->local-coords -87.02 42.5)])

;; ## Preprocessing

(def processed-trips
  (-> raw-trips
      ;; Compute starting hours:
      (tc/add-column :hour (fn [trips]
                             (->> trips
                                  :started_at
                                  (datetime/long-temporal-field :hours))))
      ;; Make sure the latitude and longitude are nonmissing:
      (tc/select-rows (fn [row]
                        (->> [:start_lat :start_lng :end_lat :end_lng]
                             (map row)
                             (every? some?))))
      ;; Add local Chicago coordinates:
      (tc/map-columns :start-local-coords [:start_lng :start_lat] lng-lat->local-coords)
      (tc/map-columns :end-local-coords [:end_lng :end_lat] lng-lat->local-coords)
      (tc/map-columns :start-local-x [:start-local-coords] first)
      (tc/map-columns :start-local-y [:start-local-coords] second)
      (tc/map-columns :end-local-x [:end-local-coords] first)
      (tc/map-columns :end-local-y [:end-local-coords] second)))

;; ## Comparing Eucledian (L2) distances in global and local coordinates

(-> processed-trips
    (tc/random 1000 {:seed 1})
    (tc/add-column :local-L2 #(fun/sqrt
                               (fun/+ (fun/sq (fun/- (:start-local-x %)
                                                     (:end-local-x %)))
                                      (fun/sq (fun/- (:start-local-y %)
                                                     (:end-local-y %))))))
    (tc/add-column :global-L2 #(fun/sqrt
                                (fun/+ (fun/sq (fun/- (:start_lat %)
                                                      (:end_lat %)))
                                       (fun/sq (fun/- (:start_lng %)
                                                      (:end_lng %))))))
    (hanami/plot ht/point-chart
                 {:X :local-L2
                  :Y :global-L2}))

;; The `local-L2` quantity, compted from the local Chicago coordinates,
;; is a decent approximation of actual distance.
;; We see that the `global-L2` quantity, computed accordingly
;; from latitude and longitude, is not directly related to
;; this quantity and might be misleading for metric tasks
;; such as clustering.

;; ## Basic analysis and visualization

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
      (hanami/plot ht/rule-chart
                   {:X :start-local-x
                    :Y :start-local-y
                    :X2 :end-local-x
                    :Y2 :end-local-y
                    :XSCALE {:zero false}
                    :YSCALE {:zero false}})))


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

;; ## Clustering

(def clustering
  (-> processed-trips
      (tc/select-columns [:start-local-x
                          :start-local-y
                          :end-local-x
                          :end-local-y])
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
       (tc/head 20))
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
