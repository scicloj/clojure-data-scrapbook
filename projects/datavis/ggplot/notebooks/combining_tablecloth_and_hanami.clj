(ns combining-tablecloth-and-hanami
  (:require [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.noj.v1.paths :as paths]
            [scicloj.tempfiles.api :as tempfiles]
            [tablecloth.api :as tc]
            [tablecloth.pipeline :as tcpipe]
            [tablecloth.column.api :as tcc]
            [tech.v3.dataset :as ds]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.metamorph.ml.toydata :as toydata]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.dataset.modelling :as modelling]
            [scicloj.metamorph.ml :as ml]
            [scicloj.metamorph.core :as mm]))


(defn make [{:keys [kindly/f]
             :as value}]
  (-> value
      (dissoc :kindly/f)
      f))

(delay
  (make {:kindly/f tc/dataset
         :x (range 4)
         :y (repeatedly 4 rand)}))


(defn prepare-data-for-vega [dataset]
  (when dataset
    (let [{:keys [path _]}
          (tempfiles/tempfile! ".csv")]
      (-> dataset
          (ds/write! path))
      (slurp path)
      {:values (slurp path)
       :format {:type "csv"}})))


(delay
  (-> {:x (range 4)
       :y (repeatedly 4 rand)}
      tc/dataset
      prepare-data-for-vega))


(delay
  (hc/xform (merge ht/view-base
                   {:data :CSVDATA
                    ::ht/defaults {:CSVDATA (comp prepare-data-for-vega :hana/data)}})
            {:hana/data (tc/dataset {:x (range 4)})}))


(delay
  (hc/xform (merge ht/view-base
                   {:data :CSVDATA
                    ::ht/defaults {:CSVDATA (fn [{:keys [hana/data
                                                         hana/stat]}]
                                              (-> data
                                                  stat
                                                  prepare-data-for-vega))}})
            {:hana/data (tc/dataset {:x (range 40)})
             :hana/stat tc/head}))


(delay
  (kind/vega-lite
   (hc/xform (merge ht/point-chart
                    {:data :CSVDATA
                     ::ht/defaults {:CSVDATA (fn [{:keys [hana/data
                                                          hana/stat]}]
                                               (-> data
                                                   stat
                                                   prepare-data-for-vega))}})
             {:hana/data (toydata/iris-ds)
              :hana/stat #(tc/head % 20)
              :X "sepal_width"
              :Y "sepal_length"
              :SIZE 100})))


(deftype WrappedValue [value]
  clojure.lang.IDeref
  (deref [this] value))

(delay
  @(->WrappedValue 9))

(delay
  (-> ht/point-chart
      (merge
       {:data :CSVDATA
        ::ht/defaults {:CSVDATA (fn [{:keys [hana/data
                                             hana/stat]}]
                                  (-> @data
                                      stat
                                      prepare-data-for-vega))
                       :X "sepal_width"
                       :Y "sepal_length"
                       :SIZE 100
                       :hana/stat #(tc/head % 20)
                       :hana/data (->WrappedValue (toydata/iris-ds))}})
      hc/xform
      kind/vega-lite))


(defn update-data [template dataset-fn & args]
  (-> template
      (update-in [::ht/defaults :hana/data]
                 (fn [wrapped-data]
                   (->WrappedValue
                    (apply dataset-fn
                           @wrapped-data
                           args))))))



(delay
  (-> ht/point-chart
      (merge
       {:data :CSVDATA
        ::ht/defaults {:CSVDATA (fn [{:keys [hana/data
                                             hana/stat]}]
                                  (-> @data
                                      stat
                                      prepare-data-for-vega))
                       :X "sepal_width"
                       :Y "sepal_length"
                       :SIZE 100
                       :hana/stat #(tc/head % 20)
                       :hana/data (->WrappedValue (toydata/iris-ds))}})
      (update-data tc/head 5)
      hc/xform
      kind/vega-lite))


(delay
  (-> {:template ht/point-chart
       :args {:X :sepal_width_2
              :Y :sepal_length
              :MSIZE 200
              :hana/stat (fn [context]
                           (update context :metamorph/data
                                   tc/sq :sepal_width_2 :sepal_width))}
       :metamorph/data (toydata/iris-ds)}
      ((mm/lift tc/random 20)))
  xform)



(-> (merge ht/point-chart
           {::ht/defaults
            {:X :sepal_width_2
             :Y :sepal_length
             :MSIZE 200
             :hana/stat (fn [context]
                          (update context :metamorph/data
                                  tc/sq :sepal_width_2 :sepal_width))
             :metamorph/data (toydata/iris-ds)}})
    hc/xform)








(defn safe-update [m k f]
  (if (m k)
    (update m k f)
    m))



(defn layered-xform [{:as context
                      :keys [template args metamorph/data]}]
  (-> context
      (update :template
              safe-update
              :layer
              (partial
               mapv
               (fn [layer]
                 (-> layer
                     ;; merge the toplevel args
                     ;; with the layer's
                     ;; specific args
                     (update :args (partial merge args))
                     (update :metamorph/data #(or % data))
                     xform))))
      xform))

(defn svg-rendered [vega-lite-template]
(assoc vega-lite-template
       :usermeta
       {:embedOptions {:renderer :svg}}))

(def view-base (svg-rendered ht/view-base))
(def point-chart (svg-rendered ht/point-chart))
(def line-chart (svg-rendered ht/line-chart))
(def point-layer ht/point-layer)
(def line-layer ht/line-layer)

(defn plot
([dataset args]
 (plot dataset
       view-base
       args))
([dataset template args]
 (kind/fn {:kindly/f layered-xform
           :metamorph/data dataset
           :template template
           :args args})))


(delay
(-> (toydata/iris-ds)
    (plot point-chart
          {:X :sepal_width
           :Y :sepal_length
           :MSIZE 200})))


(defn layer
  ([context template args]
   (if (tc/dataset? context)
     (layer (plot context {})
            template
            args)
     (-> context
         (update
          :template
          update
          :layer
          (comp vec conj)
          {:template template
           :args args})))))


(delay
  (-> (toydata/iris-ds)
      (plot {:TITLE "dummy"
             :MCOLOR "green"
             :X :sepal_width
             :Y :sepal_length})
      (layer point-layer
             {:MSIZE 100})
      (layer line-layer
             {:MSIZE 4
              :MCOLOR "brown"})
      ((mm/lift tc/random 20))))


(defn layer-point
  ([context]
   (layer-point context {}))
  ([context args]
   (layer context point-layer args)))

(defn layer-line
  ([context]
   (layer-line context {}))
  ([context args]
   (layer context line-layer args)))


(delay
  (-> (toydata/iris-ds)
      (plot {:TITLE "dummy"
             :MCOLOR "green"})
      (layer-point {:X :sepal_width
                    :Y :sepal_length
                    :MSIZE 100})
      (layer-line {:X :sepal_width
                   :Y :sepal_length
                   :MSIZE 4
                   :MCOLOR "brown"})))


(def smooth-stat
  (fn [{:as context
        :keys [template args]}]
    (let [[Y X X-predictors grouping-columns] (map args [:Y :X :X-predictors :hana/grouping-columns])
          predictors (or X-predictors [X])
          predictions-fn (fn [dataset]
                           (let [nonmissing-Y (-> dataset
                                                  (tc/drop-missing [Y]))]
                             (if (-> predictors count (= 1))
                               ;; simple linear regression
                               (let [model (fun/linear-regressor (-> predictors first nonmissing-Y)
                                                                 (nonmissing-Y Y))]
                                 (->> predictors
                                      first
                                      dataset
                                      (map model)))
                               ;; multiple linear regression
                               (let [_ (require 'scicloj.ml.smile.regression)
                                     model (-> nonmissing-Y
                                               (modelling/set-inference-target Y)
                                               (tc/select-columns (cons Y predictors))
                                               (ml/train {:model-type
                                                          :smile.regression/ordinary-least-square}))]
                                 (-> dataset
                                     (tc/drop-columns [Y])
                                     (ml/predict model)
                                     (get Y))))))
          update-data-fn (fn [dataset]
                           (if grouping-columns
                             (-> dataset
                                 (tc/group-by grouping-columns)
                                 (tc/add-or-replace-column Y predictions-fn)
                                 tc/ungroup)
                             (-> dataset
                                 (tc/add-or-replace-column Y predictions-fn))))]
      (-> context
          (update :metamorph/data update-data-fn)))))



(defn layer-smooth
  ([context]
   (layer-smooth context {}))
  ([context args]
   (layer context
          line-layer
          (merge {:hana/stat smooth-stat}
                 args))))


(delay
  (-> (toydata/iris-ds)
      (plot {:X :sepal_width
             :Y :sepal_length})
      layer-point
      layer-smooth))

(delay
  (-> (toydata/iris-ds)
      (plot {:X :sepal_width
             :Y :sepal_length
             :COLOR "species"
             :hana/grouping-columns [:species]})
      layer-point
      layer-smooth))

(delay
  (-> (toydata/iris-ds)
      (tc/concat (tc/dataset {:sepal_width (range 4 10)
                              :sepal_length (repeat 6 nil)}))
      (tc/map-columns :relative-time
                      [:sepal_length]
                      #(if % "Past" "Future"))
      (plot {:X :sepal_width
             :Y :sepal_length
             :COLOR "relative-time"})
      layer-point
      layer-smooth))


(delay
  (-> (toydata/iris-ds)
      (plot {:X :sepal_width
             :Y :sepal_length})
      layer-point
      (layer-smooth {:X-predictors [:petal_width
                                    :petal_length]})))




;; TODO: grouped-dataset, facets, hconcat, vconcat
