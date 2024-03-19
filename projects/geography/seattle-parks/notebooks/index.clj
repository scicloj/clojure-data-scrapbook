(load-file "../../../header.edn")

;; # Seattle Parks and Neighborhoods - DRAFT

;; Timothy Prately and Daniel Slutsky

;; (Probably, this notebook will be divided into a few book chapters.)

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
            [charred.api :as charred]
            [clojure2d.color :as color]
            [clojure.string :as str])
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

(md "Here is how we may generate a Choroplet map in [Leaflet](https://leafletjs.com/):")

(defn choropleth-map [details]
  (delay
    (kind/reagent
     ['(fn [{:keys [provider
                    center
                    enriched-features]}]
         [:div
          {:style {:height "900px"}
           :ref   (fn [el]
                    (let [m (-> js/L
                                (.map el)
                                (.setView (clj->js center)
                                          11))]
                      (-> js/L
                          .-tileLayer
                          (.provider provider)
                          (.addTo m))
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
                          (.addTo m))))}])
      details]
     {:reagent/deps [:leaflet]})))

;; We pick a tile layer provider from
;; [leaflet-providers](https://github.com/leaflet-extras/leaflet-providers).

(defn Seattle-choropleth-map [enriched-features]
  (choropleth-map
   {:provider "OpenStreetMap.Mapnik"
    :center     Seattle-center
    :enriched-features enriched-features}))

(md "For our basic neighborhoods map:")

(delay
  (Seattle-choropleth-map
   neighborhoods-enriched-features))


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


#_(delay
    (Seattle-choropleth-map
     parks-enriched-features))

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
      (geojson->dataset "Seattle neighborhoods")
      (tc/add-column :feature neighborhoods-enriched-features)))

(delay
  (-> neighborhoods
      (tc/drop-columns [:geometry :feature])
      (kind/table {:element/max-height "600px"})))

(def parks
  (-> parks-features
      (geojson->dataset "Seattle parks")
      ;; avoiding some [linestring pathologies](https://gis.stackexchange.com/questions/50399/fixing-non-noded-intersection-problem-using-postgis)
      (tc/map-columns :geometry [:geometry] #(buffer % 1))))

(delay
  (-> parks
      (tc/drop-columns [:geometry :feature])
      (tc/head 20)
      (kind/table {:element/max-height "600px"})))

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
      (tc/select-columns [:L_HOOD :S_HOOD :n-parks])
      (kind/table {:element/max-height "600px"})))


(md "## Computing areas

For every neighborhood, we will compute the proportion of its area covered by parks.")

;; TODO: L_HOOD should be used, S_HOOD produces too many rows to be understood
;; (maybe S_HOOD would be interesting for looking at one L_HOOD at a time)

(def neighborhoods-with-park-proportions
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
      (tc/map-columns :park-names-str
                      [:park-names]
                      (partial str/join ","))
      (tc/add-column :park-proportion
                     #(fun// (:intersection-area %)
                             (:neighborhood-area %)))))

(delay
  (-> neighborhoods-with-park-proportions
      (tc/select-columns [:L_HOOD
                          :S_HOOD
                          :park-names
                          :neighborhood-area
                          :intersection-area
                          :park-proportion])
      (tc/order-by [:park-proportion] :desc)
      (kind/table {:element/max-height "600px"})))


(defn plot-neighborhoods-with-park-proportions [data]
  (kind/echarts
   {:title {:text "Neighborhoods by park proportion"}
    :tooltip {}
    :xAxis {:data (:S_HOOD data)
            :axisLabel {:rotate 90}}
    :yAxis {}
    :series [{:type "bar"
              :data (:park-proportion data)}]
    :grid {:containLabel true}}))

(delay
  (-> neighborhoods-with-park-proportions
      (tc/order-by [:park-proportion] :desc)
      plot-neighborhoods-with-park-proportions))

;; Note you may hover to see the neighborhood names.
;; Let us take the ten most park-intense neighborhoods.

(delay
  (-> neighborhoods-with-park-proportions
      (tc/order-by [:park-proportion] :desc)
      (tc/head 10)
      plot-neighborhoods-with-park-proportions))

;; ## Representing park proportions as colors:

;; We will use Clojure2d's [clojure.color](https://clojure2d.github.io/clojure2d/docs/notebooks/index.html#/notebooks/color.clj) functionality.

(def gradient
  (color/gradient [:purple :yellow]))

(delay
  (-> 0.4
      gradient
      color/format-hex))

;; ## A choropleth coloured by park proportions

(def neighborhoods-coloured-by-park-proportion
  (-> neighborhoods-with-park-proportions
      (tc/map-columns :feature
                      [:feature :park-names-str :park-proportion]
                      (fn [feature park-names-str park-proportion]
                        (let [color (-> park-proportion
                                        gradient
                                        color/format-hex)]
                          (-> feature
                              (update-in
                               [:properties :style]
                               (fn [style]
                                 (-> style
                                     (assoc :color color
                                            :fillColor color
                                            :opacity 0.7
                                            :fillOpacity 0.7))))
                              (update-in
                               [:properties :tooltip]
                               str
                               (hiccup/html
                                [:div
                                 [:p [:b "Park proportion: "]
                                  (format "%.01f%%"
                                          (* 100 park-proportion))]
                                 [:p {:style {:max-width "200px"
                                              :text-wrap :wrap}}
                                  [:b "Parks: "]
                                  park-names-str]]))))))))

(delay
  (-> neighborhoods-coloured-by-park-proportion
      :feature
      vec
      Seattle-choropleth-map))

(md "Another option: use opacity rather than color to higlight differences.")

(def neighborhoods-highlighted-by-park-proportion
  (-> neighborhoods-coloured-by-park-proportion
      (tc/map-columns :feature
                      [:feature :park-proportion]
                      (fn [feature park-proportion]
                        (-> feature
                            (update-in
                             [:properties :style]
                             (fn [style]
                               (-> style
                                   (assoc :color "yellow"
                                          :opacity park-proportion
                                          :fillColor "yellow"
                                          :fillOpacity park-proportion)))))))))



(delay
  (-> neighborhoods-highlighted-by-park-proportion
      :feature
      vec
      Seattle-choropleth-map))


(md "In this case the choropleth mainly helps us in
pointing out a few park-intense neighborhoods.")

(md "Let us focus on the ten most park-intense neighborhoods:")

(delay
  (-> neighborhoods-coloured-by-park-proportion
      (tc/order-by [:park-proportion] :desc)
      (tc/head 10)
      :feature
      vec
      Seattle-choropleth-map))

(kind/md
 "TODO: The area proportions would be best represented in a bar chart to accompany the table
TODO: summarizing the quartiles of values might be useful as well

TODO: An interesting map would show just one L_HOOD and all the parks,
perhaps choosing the \"winner\" with most park space and showing where in Seattle it is, and what parks are there.

## Conclusion

Park access is fairly uniform and high in Seattle.
TODO: can we compare it to another city?
"
 )


(md "
## Draft text for the story to be told.

- Intersections and spatial joins

- In what context would these visualizations be useful?

- Using the REPL to dive into cases and their details
- Doing that in self-documenting way
- How does this compose with GUI-based explorations?

- Choice of colors
")
