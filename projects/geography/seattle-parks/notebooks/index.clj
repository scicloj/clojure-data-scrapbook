(load-file "../../../header.edn")

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
            [hiccup.core :as hiccup]
            [charred.api :as charred])
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
;; [Seattle GeoData](https://data-seattlecitygis.opendata.arcgis.com/):
;;
;; - ["Park Boundary" dataset](https://data-seattlecitygis.opendata.arcgis.com/datasets/94e59cd6e7a6479c9131cc3eb40b29b8_2/explore)
;;
;; - ["Neighborhoods Map Atlas Neighborhoods" dataset](https://data-seattlecitygis.opendata.arcgis.com/datasets/neighborhood-map-atlas-neighborhoods/)
;;
;; I've saved a snapshot in the `data` directory.
;;
;; The data format is gzipped [GeoJSON](https://en.wikipedia.org/wiki/GeoJSON).
;; Java has a built-in class for handling gzip streams,
;; and we'll use the [factual/geojson](https://github.com/Factual/geo) library to parse the string representation.

(defn slurp-gzip
  "Read a gzipped file into a string"
  [path]
  (with-open [in (java.util.zip.GZIPInputStream. (clojure.java.io/input-stream path))]
    (slurp in)))

;; Now we can conveniently load the data files we downloaded previously.

(defonce neighborhoods-geojson
  (slurp-gzip "data/Seattle/Neighborhood_Map_Atlas_Neighborhoods.geojson.gz"))

(def neighborhoods-features
  (geoio/read-geojson neighborhoods-geojson))

;; Let's check that we got some data.

(count neighborhoods-features)

;; This seems like a reasonable number of neighborhoods.

;; Each member of the dataset is called a [Feature](https://en.wikipedia.org/wiki/Simple_Features). Here is one:

(-> neighborhoods-features
    first
    kind/pprint)

;; Each feature, in our case, represents a geographic region with a geometry and some properties.
;;
;; And similarly for the parks:

(defonce parks-geojson
  (slurp-gzip "data/Seattle/Park_Boundary_(details).geojson.gz"))

(def parks-features
  (geoio/read-geojson parks-geojson))

(count parks-features)

;; There are more parks than neighborhoods, which sounds right.

(delay
  (-> parks-features
      first
      kind/pprint))

;; And the parks are defined as geographic regions.

(md "## Drawing a map")

(md "[Seattle coordinates](https://www.latlong.net/place/seattle-wa-usa-2655.html)")

(def Seattle-center
  [47.608013 -122.335167])

(md "The map we will create is [A choropleth](https://en.wikipedia.org/wiki/Choropleth_map), though for now, we will use a fixed color, which is not so informative.

We will enrich every feature (e.g., neighborhood) with data relevant for its visual representation.")

(defn enrich-feature [{:as   feature :keys [geometry]}
                      {:keys [tooltip-keys
                              style]}]
  (-> feature
      (update :properties
              (fn [properties]
                (-> properties
                    (assoc :tooltip (-> properties
                                        (select-keys tooltip-keys)
                                        (->> (map (fn [[k v]]
                                                    [:p [:b k] ":  " v]))
                                             (into [:div]))
                                        hiccup/html)
                           :style style))))))

(def neighborhoods-enriched-features
  (-> neighborhoods-geojson
      (charred/read-json {:key-fn keyword})
      :features
      (->> (mapv (fn [feature]
                   (-> feature
                       (enrich-feature
                        {:tooltip-keys [:L_HOOD :S_HOOD]
                         :style {:opacity     0.3
                                 :fillOpacity 0.1
                                 :color      "purple"
                                 :fillColor  "purple"}})))))))

(md "We will need a tile layer for our visual map:")

(def openstreetmap-tile-layer
  {:url         "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
   :max-zoom    19
   :attribution "&copy;; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"})

(md "Here is how we may generate a Choroplet map in [Leaflet](https://leafletjs.com/):")

(defn choropleth-map [details]
  (delay
    (kind/reagent
     ['(fn [{:keys [tile-layer
                    center
                    enriched-features]}]
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
                      (-> js/L
                          (.geoJson (clj->js enriched-features)
                                    (clj->js {:style (fn [feature]
                                                       (-> feature
                                                           .-properties
                                                           .-style))}))
                          (.bindTooltip (fn [layer]
                                          (-> layer
                                              .-feature
                                              .-properties
                                              .-tooltip)))
                          (.addTo m))
                      #_(->> enriched-features
                             (run! (fn [{:keys [properties geometry]}]
                                     (let [{:keys [style tooltip]} properties]
                                       (case (:type geometry)
                                         "Polygon" (-> js/L
                                                       (.polygon (-> geometry
                                                                     :coordinates
                                                                     first
                                                                     clj->js)
                                                                 (-> style
                                                                     (or {})
                                                                     clj->js (or style {})))
                                                       (.bindTooltip tooltip)
                                                       (.addTo m))
                                         "MultiPolygon" (-> js/L
                                                            (.multiPolygon (-> geometry
                                                                               :coordinates
                                                                               clj->js)
                                                                           (-> style
                                                                               (or {})
                                                                               clj->js (or style {})))
                                                            (.bindTooltip tooltip)
                                                            (.addTo m))
                                         ;; else
                                         (-> geometry
                                             :type
                                             (str " - unrecognized geometry type")
                                             js/alert))))))))}])
      details]
     {:reagent/deps [:leaflet]})))


(md "For our basic neighborhoods map:")

(delay
  (choropleth-map
   {:tile-layer openstreetmap-tile-layer
    :center     Seattle-center
    :enriched-features neighborhoods-enriched-features}))


(md "Now, let us see the parks:")

(def parks-enriched-features
  (-> parks-geojson
      (charred/read-json {:key-fn keyword})
      :features
      (->> (mapv (fn [feature]
                   (-> feature
                       (enrich-feature
                        {:tooltip-keys [:PMA_NAME :NAME]
                         :style {:opacity     0.3
                                 :fillOpacity 0.1
                                 :color      "darkgreen"
                                 :fillColor  "darkgreen"}})))))))

(delay
  (choropleth-map
   {:tile-layer openstreetmap-tile-layer
    :center     Seattle-center
    :enriched-features parks-enriched-features}))




(md "## Coordinate conversions")

(md "
Both datasets use the [WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System)
coordinate system.

[EPSG:4326](https://epsg.io/4326)")


(md "For area computations we need to convert them to a coordinate system
which is locally correct in terms of distances in a region around Seattle.

[EPSG:2285](https://epsg.io/2285)")

^:kindly/hide-code
(-> {"Center coordinates" [[1692592.39 -541752.55]]
     "Projected bounds"   [[[559165.71 -1834123.81]
                            [2832684.98 777497.1]]]
     "WGS84 bounds"       [[[-124.79 41.98]
                            [-116.47 49.05]]]}
    tc/dataset
    (tc/set-dataset-name "United States (USA) - Oregon and Washington."))

(def crs-transform
  (geo.crs/create-transform (geo.crs/create-crs 4326)
                            (geo.crs/create-crs 2285)))

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
  (-> geojson
      (->> (map (fn [{:keys [geometry properties]}]
                  (assoc properties :geometry geometry))))
      tc/dataset
      (tc/map-columns :geometry [:geometry] wgs84->Seattle)
      (tc/set-dataset-name dataset-name)))

(def neighborhoods
  (-> neighborhoods-features
      (geojson->dataset "Seattle neighborhoods")))

(delay
  (-> neighborhoods
      (tc/drop-columns [:geometry])
      kind/table))

(def parks
  (-> parks-features
      (geojson->dataset "Seattle parks")
      ;; avoiding some [linestring pathologies](https://gis.stackexchange.com/questions/50399/fixing-non-noded-intersection-problem-using-postgis)
      (tc/map-columns :geometry [:geometry] #(buffer % 1))))

(delay
  (-> parks
      (tc/drop-columns [:geometry])
      (tc/head 20)
      kind/table))

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

;; For example, let us find the parks intersecting with the first neighborhood:

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
      (tc/select-columns [:L_HOOD :S_HOOD :n-parks])))


(md "## Computing areas

For every neighborhood, we will compute the proportion of its area covered by parks.")

;; TODO: L_HOOD should be used, S_HOOD produces too many rows to be understood
;; (maybe S_HOOD would be interesting for looking at one L_HOOD at a time)

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
