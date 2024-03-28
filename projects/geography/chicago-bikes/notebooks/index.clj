(load-file "../../../header.edn")

;; # Analysing Chicago bike trips

;; This notebook is the fruit of our explorations at the new
;; [real-world-data group](https://scicloj.github.io/docs/community/groups/real-world-data/).
;; It uses a few of libraries of the emerging Clojure stack for data and science:
;; Tablecloth, dtype-next, Fastmath, Hanami, Geo, and Kindly.
;;
;; Soon we will publish a video walkthrough.
;;
;; Daniel Slutsky, March 2024

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

(md
 "
In this notebook, we will explore the data of a bike sharing company in Chicago.

The data provides information of start & end location & time for each bike trip.
It has been used in academic papers and blog posts discussing various aspects of
people's movement patterns in their everyday lives.

Our goal here is to recognize and visualize a few movement patterns.

## Background

### Various openly shared datasets:
- [Divvy Trips](https://data.cityofchicago.org/Transportation/Divvy-Trips/fg6s-gzvg/about_data) - Chicago data portal
- [Chicago Divvy Bicycle Sharing Data](https://www.kaggle.com/datasets/yingwurenjian/chicago-divvy-bicycle-sharing-data) - Kaggle
- [Cyclistic bike share](https://www.kaggle.com/datasets/evangower/cyclistic-bike-share) - Kaggle
- [Cyclistic bike share - 2023 update](https://www.kaggle.com/datasets/godofoutcasts/cyclistic-bike-share-2023) - Kaggle

### Research papers and blog posts
- [Understanding Spatiotemporal Patterns of Biking Behavior by Analyzing Massive Bike Sharing Data in Chicago](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0137922) - Zhou 2015
- [Bicycle-Sharing System Analysis and Trip](https://www.cs.uic.edu/~jzhang2/files/2016_mdm_paper.pdf) - Zhang el al. 2016
- [Spatial Cluster-Based Model for Static Rebalancing Bike Sharing Problem](https://www.mdpi.com/2071-1050/11/11/3205) - Lahoorpoor et al. 2019
- [Examining spatiotemporal changing patterns of bike-sharing usage during COVID-19 pandemic](https://www.sciencedirect.com/science/article/pii/S0966692321000508) - Hu et al. 2021
- [Analysis: Chicago’s Divvy Bike Share](https://andrewluyt.github.io/divvy-bikeshare/analysis-report.html) - Luyt 2021


## Data sources for this notebook

1. [Chicago neighborhoods geojson](https://github.com/blackmad/neighborhoods/blob/master/chicago.geojson) - part of [a great collection](https://github.com/blackmad/neighborhoods/) by [@blackmad](https://github.com/blackmad).
We keep this file under \"notebooks/data/\". By default, [Clay](https://scicloj.github.io/clay) pushes data files under \"notebooks\" to the target render directory.
This will allow us to consume the file from the browser for data visualizations.

2. [Cyclistic bike share - 2023 update](https://www.kaggle.com/datasets/godofoutcasts/cyclistic-bike-share-2023) - shared on Kaggle by Jayprakash Kumar.
This dataset contains separate data files for different months. We will use only one of them here (Apr. 2023) and keep it under \"data/\".

## Reading the data

We read the raw dataset as a [Tablecloth](https://scicloj.github.io/tablecloth/) dataset
with some proper date-time parsing and column names as keywords.
")


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

(md "For computations telated to distances (e.g., k-means clustering),
we need to convert them to a coordinate system
which approximates plane geometry in a region around Chicago:
[EPSG:26971](https://epsg.io/26971)")

^:kindly/hide-code
(-> {"Center coordinates" [[316133.6 345590.74]]
     "Projected bounds"   [[[216692.35 43649.23]
                            [416809.98 648476.24]]]
     "WGS84 bounds"       [[[-89.27 37.06]
                            [-87.02 42.5]]]}
    tc/dataset
    (tc/set-dataset-name "NAD83 / Illinois East."))

(md "Let us define our coordinate transformation
using the [Geo](https://github.com/Factual/geo) library.")

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

(md "For example, let us convert some latitude-longiute pairs
to our local plane coordinates.")

(delay
  [(lng-lat->local-coords -89.27 37.06)
   (lng-lat->local-coords -87.02 42.5)])

(md "
## Preprocessing

Let us prepare our dataset for analysis.
")

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
      ;; Add local plane coordinates:
      (tc/map-columns :start-local-coords [:start_lng :start_lat] lng-lat->local-coords)
      (tc/map-columns :end-local-coords [:end_lng :end_lat] lng-lat->local-coords)
      (tc/map-columns :start-local-x [:start-local-coords] first)
      (tc/map-columns :start-local-y [:start-local-coords] second)
      (tc/map-columns :end-local-x [:end-local-coords] first)
      (tc/map-columns :end-local-y [:end-local-coords] second)))

(md "## Comparing Eucledian (L2) distances in global and local coordinates

This is a digression for the curious. It is not part of the actual analysis.

Earlier mentioned that our `x`,`y` coordinates approximately represent
for plane geometry in the relevant region, unlike the `latitude`,`longitude` ones.

Let us check how different they actually are in measuring distances.
We will sample 1000 bike trips, and for each trip, measure the
L2 distance between the start and the end.

L2 is the distance in our usual Eucledian plane geometry:
the square root of the sum of square distances,
where the sum is over our two coordinates
(as can be justified by the Pythagorean theorem).

We will compute it `x`,`y` (approximate plane coordinates),
and then in `latitude`,`longitude` (globe coordinates).
The former should be approximately correct, the latter - not so much.

Let us compare the two in a scatterplot. ")

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

(md "
Here, we used the [Noj](https://scicloj.github.io/noj/) wrapper
of [Hanami](https://github.com/jsa-aerial/hanami),
which makes it compose conveniently with Tablecloth,
eventually generating [Vega-Lite](https://vega.github.io/) plots.

The `local-L2` quantity, compted from the local Chicago coordinates,
is a decent approximation of actual distance.
We see that the `global-L2` quantity, computed accordingly
from latitude and longitude, is not directly related to
this quantity and might be misleading for metric tasks
such as clustering. They are correlated, though,
and if one of them is small, the other one is small as well.

In this section, we computed distances between pairs of coordinates.
E.g., between the `x`,`y` of the trip start, to those of the trip end.
Later, we will represent each trip by a quadruple of coordinates
(both start and end), and use the 4D geometry of those quadruples
to gather our data into clusters of similar trips.

")


(md "
## Basic analysis and visualization

### Time
Let us see how trip start hours are distributed along the day.

")

(defn hour-counts-plot [trips]
  (-> trips
      (tc/group-by [:hour])
      (tc/aggregate {:n tc/row-count})
      (hanami/plot ht/bar-chart
                   {:X :hour
                    :Y :n})))

(delay
  (hour-counts-plot processed-trips))



(md "
### Space

We wish to plot our trips as line segments
(Vega-Lite [Rules](https://vega.github.io/vega-lite/docs/rule.html))
over a map.

Hanami's templates create Vega-Lite plots with `x` and `y` coordinates,
but we need geographical plots, with `latitude` and `longitude`.

The following function will allow us to convert typical Hanami plots
to geographical ones.
")


(defn as-geo [vega-lite-spec]
  (-> vega-lite-spec
      (update :encoding update-keys (fn [k]
                                      (get {:x :latitude
                                            :y :longitude
                                            :x2 :latitude2
                                            :y2 :longitude2}
                                           k k)))
      (assoc :projection {:type :mercator})))

(md "We will find it useful to render some Vega-Lite plots
as PNG, rather than SVG. This will meke the whole page more lightweight.")

(defn as-png [vega-lite-spec]
  (-> vega-lite-spec
      (assoc :usermeta {:embedOptions {:renderer :png}})))


(md "Let us try it out with a sample of points.")

(delay
  (-> processed-trips
      (tc/random 10000 {:seed 1})
      (hanami/plot ht/rule-chart
                   {:X :start_lat
                    :Y :start_lng
                    :X2 :end_lat
                    :Y2 :end_lng
                    :OPACITY 0.1})
      as-geo
      as-png))


(md "Let us add the neighbourhood geometries as an additional layer.")

(delay
  (-> processed-trips
      (tc/random 10000 {:seed 1})
      (hanami/plot ht/layer-chart
                   {:TITLE "Chicago bike trips"
                    :LAYER [{:data {:url "notebooks/data/chicago.geojson"
                                    :format {:type "topojson"}}
                             :mark {:type "geoshape"
                                    :filled false
                                    :clip true
                                    :opacity 0.5}}
                            (as-geo
                             (hanami/plot nil ; use the global dataset for this layer
                                          ht/rule-chart
                                          {:X :start_lat
                                           :Y :start_lng
                                           :X2 :end_lat
                                           :Y2 :end_lng
                                           :OPACITY 0.1}))]})
      as-png))


;; ## Clustering

;; To recognize repeating patterns of similar trips,
;; we use [k-means clustering](https://en.wikipedia.org/wiki/K-means_clustering)
;; over trips, representing them as quadruple of points
;; using the plane coordinates of their start and end.

(def clustering
  (-> processed-trips
      (tc/select-columns [:start-local-x
                          :start-local-y
                          :end-local-x
                          :end-local-y])
      tc/rows
      (clustering/k-means 100)
      (dissoc :data)))

;; Let us plot the biggest of the resulting clusters.
;; We will plot each cluster separately in time and in space as before.
;; To the line segments over the map we add the start and end points,
;; in purple and green, respectively.


(delay
  (-> processed-trips
      (tc/add-column :cluster (:clustering clustering))
      (tc/group-by [:cluster])
      ;; sort the groups in ascending order by their size,
      ;; and take the first few (which are the largest ones)
      ;; (we use `without-grouping->` to order and take
      ;; across groups rather tan within groups):
      (tc/without-grouping->
       (tc/order-by (fn [ds]
                      (-> ds :data tc/row-count))
                    :desc)
       (tc/head 10))
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
                              [(as-png
                                (hour-counts-plot trips))])})
      kind/table))

;; Some patterns are noticible. In future explorations, we should hope to reach
;; some interpretations regarding repeating movement in certain hours,
;; such as the end of the work day, in relation to certain destinations,
;; like major train stations.
;; We sould certainly hope to discuss them with local friends who
;; know something about this area.
;;
;; This can be considered a first step in reproducing some of the methods
;; of the existing insightful articles cited above.

;; ## Conclusion, for now

;; For now, we will stop here.

;; This has mostly been a demonstration of a few libraries and practices
;; which are central to the emerging Clojure platform for data and science.

;; We have also had a glance into a rich dataset which can provide fruitful
;; examples for our future explorations, demonstrating nontrivial
;; phenomena in space and in time.
