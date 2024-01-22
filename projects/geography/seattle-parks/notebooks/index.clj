;; # Seattle Parks & Neighborhoods

;; Choosing where to live depends on many factors such as job opportunities and cost of living.
;; I like walking, so one factor that is important to me is access to parks.
;; In this analysis we'll rank neighborhoods by park area proportional to total area.
;; This article demonstrates how to prepare the geospatial data, calculate the value we want,
;; and how to explore the meaning behind the numbers.

(ns index
  (:require [geo
             [geohash :as geohash]
             [jts :as jts]
             [spatial :as spatial]
             [io :as geoio]
             [crs :as crs]]
            [tech.v3.datatype.functional :as fun]
            [tablecloth.api :as tc]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]
            [hiccup.core :as hiccup])
  (:import (org.locationtech.jts.index.strtree STRtree)
           (org.locationtech.jts.geom Geometry Point Polygon Coordinate)
           (org.locationtech.jts.geom.prep PreparedGeometry
                                           PreparedLineString
                                           PreparedPolygon
                                           PreparedGeometryFactory)
           (java.util TreeMap)))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

;; ## Gathering geospatial data

;; Both the neighborhood geometry and park geometry can be downloaded from
;; ![Seattle GeoData](https://data-seattlecitygis.opendata.arcgis.com/datasets/SeattleCityGIS::neighborhood-map-atlas-neighborhoods/explore)

;; I've saved a snapshot in the `data` directory.



;; The data format is gzipped geojson.
;; Java has a built-in class for handling gzip streams,
;; and we'll use the [factual/geojson](https://github.com/Factual/geo) library to parse the string representation.

(defn slurp-gzip
  "Read a gzipped file into a string"
  [path]
  (with-open [in (java.util.zip.GZIPInputStream. (clojure.java.io/input-stream path))]
    (slurp in)))

(defn parse-geojson-gz
  "Read a gzipped GeoJSON file."
  [path]
  (-> (slurp-gzip path)
      (geo.io/read-geojson)))

;;; Now we can conveniently load the data files we downloaded previously.

(def neighborhoods-geojson
  (parse-geojson-gz "data/Seattle/Neighborhood_Map_Atlas_Neighborhoods.geojson.gz"))

;;; Let's check that we got some data

(count neighborhoods-geojson)

;;; This seems like a reasonable number of neighborhoods

(-> (first neighborhoods-geojson)
    (kind/pprint))

;;; The data itself consists of geographic regions

;;; And similarly for the parks

(def parks-geojson
  (parse-geojson-gz "data/Seattle/Park_Boundary_(details).geojson.gz"))

(count parks-geojson)

;;; There are more parks than suburbs, which sounds right

(-> (first parks-geojson)
    (kind/pprint))

;;; And the parks are defined as geographic regions

(md "## Drawing a map")

(kind/md "[Seattle](https://www.latlong.net/place/seattle-wa-usa-2655.html)")

(def Seattle-center
  [47.608013 -122.335167])

(def neighborhoods-coordinates
  (->> neighborhoods-geojson
       (mapv (fn [{:as   feature
                   :keys [geometry]}]
               {:shape-type  :polygon
                :coordinates (->> geometry
                                  geo.jts/coordinates
                                  (mapv (fn [c]
                                          [(.y c) (.x c)])))
                :tooltip     (-> feature
                                 :properties
                                 (select-keys [:L_HOOD])
                                 (->> (map (fn [[k v]]
                                             [:p [:b k] ":  " v]))
                                      (into [:div]))
                                 hiccup/html)}))))

(->> neighborhoods-coordinates
     (take 5)
     kind/portal)

(kind/reagent
  ['(fn [{:keys [tile-layer
                 center
                 shapes]}]
      [:div
       {:style {:height "900px"}
        :ref   (fn [el]
                 (let [m (-> js/L
                             (.map el)
                             (.setView (clj->js center)
                                       11))]
                   (let [{:keys [url max-zoom attribution]}
                         tile-layer]
                     (-> js/L
                         (.tileLayer url
                                     (clj->js
                                       {:maxZoom     max-zoom
                                        :attribution attribution}))
                         (.addTo m)))
                   (->> shapes
                        (run! (fn [{:keys [shape-type
                                           coordinates
                                           style
                                           tooltip]}]
                                (case shape-type
                                  :polygon (-> js/L
                                               (.polygon (clj->js coordinates)
                                                         (clj->js (or style {})))
                                               (.bindTooltip tooltip)
                                               (.addTo m))))))))}])
   {:tile-layer {:url         "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                 :max-zoom    19
                 :attribution "&copy;; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"}
    :center     Seattle-center
    :shapes     (->> neighborhoods-coordinates
                     (mapv #(assoc % :style {:opacity     0.3
                                             :fillOpacity 0.1})))}]
  {:reagent/deps [:leaflet]})


(md "## Coordinate conversions")

(md "
Both datasets use the [WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System)
coordinate system.
For area computations we need to convert them to a coordinate system
which is locally correct in terms of distances in a region around Seattle. ")

;;

;;; [EPSG:4326](https://epsg.io/4326)
(def wgs84-crs (geo.crs/create-crs 4326))

(require '[tablecloth.api :as tc])

;;; [EPSG:2285](https://epsg.io/2285)
^:kindly/hide-code
(-> {"Center coordinates" [[1692592.39 -541752.55]]
     "Projected bounds"   [[[559165.71 -1834123.81]
                            [2832684.98 777497.1]]]
     "WGS84 bounds"       [[[-124.79 41.98]
                            [-116.47 49.05]]]}
    tc/dataset
    (tc/set-dataset-name "United States (USA) - Oregon and Washington."))

(def Seattle-crs (geo.crs/create-crs 2285))

(def crs-transform
  (geo.crs/create-transform wgs84-crs Seattle-crs))

(defn wgs84->Seattle
  "Transforming latitude-longitude coordinates
  to local Euclidean coordinates around Seattle."
  [geometry]
  (geo.jts/transform-geom geometry crs-transform))

(md "## Some geometrical functions")

(defn area [geometry]
  (.getArea geometry))

(defn buffer [geometry radius]
  (.buffer geometry radius))

(defn intersection [geometry1 geometry2]
  (.intersection geometry1 geometry2))

(md "## Geometry datasets")

(defn geojson->dataset [geojson dataset-name]
  (-> (map (fn [{:keys [geometry properties]}]
             (assoc properties :geometry geometry))
           geojson)
      (tc/dataset)
      (tc/map-columns :geometry [:geometry] wgs84->Seattle)
      (tc/set-dataset-name dataset-name)))

(def neighborhoods
  (geojson->dataset neighborhoods-geojson "Seattle neighborhoods"))

(tc/drop-columns neighborhoods [:geometry])

(def parks
  (-> (geojson->dataset parks-geojson "Seattle parks")
      ;;; avoiding some [linestring pathologies](https://gis.stackexchange.com/questions/50399/fixing-non-noded-intersection-problem-using-postgis)
      (tc/map-columns :geometry [:geometry] #(buffer % 1))))

(delay
  (-> (tc/drop-columns parks [:geometry])
      (tc/head 20)))

(md "## A Spatial index structure

We need an index structure to quickly match between the two sets of geometries.

See the JTS [SearchUsingPreparedGeometryIndex tutorial](https://github.com/locationtech/jts/blob/master/modules/example/src/main/java/org/locationtech/jtsexample/technique/SearchUsingPreparedGeometryIndex.java). ")


(defn make-spatial-index [dataset & {:keys [geometry-column]
                                     :or   {geometry-column :geometry}}]
  (let [tree (org.locationtech.jts.index.strtree.STRtree.)]
    (doseq [row (tc/rows dataset :as-maps)]
      (let [geometry (row geometry-column)]
        (.insert tree
                 (.getEnvelopeInternal geometry)
                 (assoc row
                   :prepared-geometry
                   (org.locationtech.jts.geom.prep.PreparedGeometryFactory/prepare geometry)))))
    tree))

(def parks-index
  (make-spatial-index parks))

(defn intersecting-places [region spatial-index]
  (->> (.query spatial-index (.getEnvelopeInternal region))
       (filter (fn [row]
                 (.intersects (:prepared-geometry row) region)))
       tc/dataset))

;;; For example, let us find the parks intersecting with the first neighborhood:

(delay
  (-> neighborhoods
      :geometry
      first
      (intersecting-places parks-index)
      (tc/select-columns [:PMA_NAME :NAME])))

(md "## A Joined dataset

We compute [a spatial join](https://en.wikipedia.org/wiki/Spatial_join)
of the two datasets.

Note that even though many parks will appear as intersecting many neighbourhoods, this is not too memory-heavy, since they are references to the same map.")

(def neighborhoods-with-parks
  (-> neighborhoods
      (tc/map-columns :parks
                      [:geometry]
                      #(intersecting-places % parks-index))))


(delay
  (-> neighborhoods-with-parks
      (tc/map-columns :n-parks
                      [:parks]
                      tc/row-count)
      (tc/select-columns [:L_HOOD :n-parks])))


(md "## Computing areas

For every neighborhood, we will compute the proportion of its area covered by parks.")

(require '[tech.v3.datatype.functional :as fun])

;;; TODO: L_HOOD should be used, S_HOOD produces too many rows to be understood
;;; (maybe S_HOOD would be interesting for looking at one L_HOOD at a time)

(delay
  (-> neighborhoods-with-parks
      (tc/map-columns :neighborhood-area
                      [:geometry]
                      area)
      (tc/map-columns :intersection-area
                      [:geometry :parks]
                      (fn [neigh-geometry parks]
                        (->> parks
                             :geometry
                             (map (fn [park-geometry]
                                    (area
                                      (.intersection (.buffer park-geometry 1)
                                                     neigh-geometry))))
                             fun/sum)))
      (tc/map-columns :park-names
                      [:parks]
                      (fn [parks]
                        (->> parks
                             :PMA_NAME
                             distinct
                             vec)))
      (tc/add-column :area-proportion
                     #(fun// (:intersection-area %)
                             (:neighborhood-area %)))
      (tc/select-columns [:L_HOOD
                          :park-names
                          :neighborhood-area
                          :intersection-area
                          :area-proportion])
      (tc/order-by [:area-proportion] :desc)))

;; TODO: The area proportions would be best represented in a bar chart to accompany the table
;; TODO: summarizing the quartiles of values might be useful as well

;; TODO: An interesting map would show just one L_HOOD and all the parks,
;; perhaps choosing the "winner" with most park space and showing where in Seattle it is, and what parks are there.

;; ## Conclusion

;; Park access is fairly uniform and high in Seattle.
;; TODO: can we compare it to another city?
