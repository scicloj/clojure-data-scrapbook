;; # Seattle Parks & Neighborhoods
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
            [scicloj.kindly.v4.api :as kindly])
  (:import (org.locationtech.jts.index.strtree STRtree)
           (org.locationtech.jts.geom Geometry Point Polygon Coordinate)
           (org.locationtech.jts.geom.prep PreparedGeometry
                                           PreparedLineString
                                           PreparedPolygon
                                           PreparedGeometryFactory)
           (java.util TreeMap)))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

(md "
In this analysis we rank Seattle neighborhoods
by the presence of of Parks in them
(in terms of area proportion).")

(md "## Raw geo data")

(defn read-geogson-gz
  "Read a gzipped GeoJSON file."
  [path]
  ;; See [Reading and Writing Compressed Files](Reading and Writing Compressed Files) by John Cromartie.
  (with-open [in (java.util.zip.GZIPInputStream.
                  (clojure.java.io/input-stream
                   path))]
    (-> in
        slurp
        geoio/read-geojson)))

(def neighborhoods-geojson
  (read-geogson-gz
   "data/Seattle/Neighborhood_Map_Atlas_Neighborhoods.geojson.gz"))

(count neighborhoods-geojson)

(->> neighborhoods-geojson
     (take 5)
     kind/portal)

(def parks-geojson
  (read-geogson-gz
   "data/Seattle/Park_Boundary_(details).geojson.gz"))

(count parks-geojson)

(->> parks-geojson
     (take 5)
     kind/portal)

(md "## Drawing a map")

(kind/md "[Seattle](https://www.latlong.net/place/seattle-wa-usa-2655.html)")

(def Seattle-center
  [47.608013 -122.335167])

(def neighborhoods-coordinates
  (->> neighborhoods-geojson
       (mapv (fn [{:keys [geometry]}]
               {:shape-type :polygon
                :coordinates (->> geometry
                                  jts/coordinates
                                  (mapv (fn [^Coordinate c]
                                          [(.y c) (.x c)])))}))))

(->> neighborhoods-coordinates
     (take 5)
     kind/portal)

(kind/reagent
 ['(fn [{:keys [tile-layer
                center
                shapes]}]
     [:div
      {:style {:height "600px"}
       :ref (fn [el]
              (let [m (-> js/L
                          (.map el)
                          (.setView (clj->js center)
                                    11))]
                (let [{:keys [url max-zoom attribution]}
                      tile-layer]
                  (-> js/L
                      (.tileLayer url
                                  (clj->js
                                   {:maxZoom max-zoom
                                    :attribution attribution}))
                      (.addTo m)))
                (->> shapes
                     (run! (fn [{:keys [shape-type coordinates]}]
                             (case shape-type
                               :polygon (-> js/L
                                            (.polygon (clj->js coordinates))
                                            (.addTo m)) ))))))}])
  {:tile-layer {:url "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                :max-zoom 19
                :attribution "&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"}
   :center Seattle-center
   :shapes neighborhoods-coordinates}]
 {:reagent/deps [:leaflet]})


(md "## Coordinate conversions")

(md "
Both datasets use the [WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System)
coordinate system.
For area computations, etc., we need to convert them to a coordinate system
which is locally correct in terms of distances in a region around Seattle. ")

;;

;; [EPSG:4326](https://epsg.io/4326)
(def wgs84-crs (crs/create-crs 4326))

;; [EPSG:2285](https://epsg.io/2285)
^:kindly/hide-code
(-> {"Center coordinates" [[1692592.39 -541752.55]]
     "Projected bounds" [[[559165.71 -1834123.81]
                          [2832684.98 777497.1]]]
     "WGS84 bounds" [[[-124.79 41.98]
                      [-116.47 49.05]]]}
    tc/dataset
    (tc/set-dataset-name "United States (USA) - Oregon and Washington."))

(def Seattle-crs (crs/create-crs 2285))

(def crs-transform
  (crs/create-transform wgs84-crs Seattle-crs))

(defn wgs84->Seattle
  "Transforming latitude-longitude coordinates
  to local Eucledian coordinates around Seattle."
  [geometry]
  (jts/transform-geom geometry crs-transform))

(md "## Some geometrical functions")

(defn area [^Geometry geometry]
  (.getArea geometry))

(defn buffer [^Geometry geometry radius]
  (.buffer geometry radius))

(defn intersection [^Geometry geometry1
                    ^Geometry geometry2]
  (.intersection geometry1
                 geometry2))

(md "## Preproessed datasets")

(defn geojson->dataset [geojson dataset-name]
  (-> geojson
      (->> (map (fn [{:keys [geometry properties]}]
                  (assoc properties :geometry geometry))))
      tc/dataset
      (tc/map-columns :geometry [:geometry]
                      wgs84->Seattle)
      (tc/set-dataset-name dataset-name)))

(def neighborhoods
  (-> neighborhoods-geojson
      (geojson->dataset "Seattle neighborhoods")))

(-> neighborhoods
    (tc/drop-columns [:geometry])
    kind/table)

(def parks
  (-> parks-geojson
      (geojson->dataset "Seattle parks")
      (tc/map-columns :geometry
                      [:geometry]
                      ;; avoiding some [linestring pathologies](https://gis.stackexchange.com/questions/50399/fixing-non-noded-intersection-problem-using-postgis)
                      #(buffer % 1))))

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
  (let [tree ^STRtree (STRtree.)]
    (doseq [row (tc/rows dataset :as-maps)]
      (let [geometry ^Geometry (row geometry-column)]
        (.insert tree
                 (.getEnvelopeInternal geometry)
                 (assoc row
                        :prepared-geometry
                        (PreparedGeometryFactory/prepare geometry)))))
    tree))

(def parks-index
  (make-spatial-index parks))


(defn intersecting-places [^Geometry region spatial-index]
  (->> (.query ^STRtree spatial-index
               ^Envelope (.getEnvelopeInternal ^Geometry region))
       (filter (fn [row]
                 (.intersects ^PreparedGeometry (:prepared-geometry row)
                              region)))
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
      (tc/select-columns [:L_HOOD :S_HOOD :S_HOOD_ALT_NAMES
                          :n-parks])))


(md "## Computing areas
For evey neigbourhood, we will compute the proportion of its area covered by parks.")

(delay
  (-> neighborhoods-with-parks
      (tc/map-columns :neighborhood-area
                      [:geometry]
                      area)
      (tc/map-columns :intersection-area
                      [:geometry :parks]
                      (fn [^Geometry neigh-geometry parks]
                        (->> parks
                             :geometry
                             (map (fn [^Geometry park-geometry]
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
      (tc/select-columns [:L_HOOD :S_HOOD :S_HOOD_ALT_NAMES
                          :park-names
                          :neighborhood-area
                          :intersection-area
                          :area-proportion])
      (tc/order-by [:area-proportion] :desc)
      kind/table))
