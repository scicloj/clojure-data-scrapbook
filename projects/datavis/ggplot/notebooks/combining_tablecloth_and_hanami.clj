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


(defn dataset->csv [dataset]
  (when dataset
    (let [{:keys [path _]}
          (tempfiles/tempfile! ".csv")]
      (-> dataset
          (ds/write! path))
      (slurp path))))

(delay
  (-> {:x (range 4)
       :y (repeatedly 4 rand)}
      tc/dataset
      dataset->csv))


(delay
  (hc/xform (merge ht/view-base
                   {::ht/defaults {:VALDATA (comp dataset->csv :hana/data)
                                   :DFMT {:type "csv"}}})
            {:hana/data (tc/dataset {:x (range 4)})}))


(delay
  (hc/xform (merge ht/view-base
                   {::ht/defaults {:VALDATA (fn [{:as args
                                                  :keys [hana/stat]}]
                                              (-> args
                                                  stat
                                                  dataset->csv))
                                   :DFMT {:type "csv"}}})
            {:hana/data (tc/dataset {:x (range 40)})
             :hana/stat (comp tc/head :hana/data)}))


(delay
  (kind/vega-lite
   (hc/xform (merge ht/point-chart
                    {::ht/defaults {:VALDATA (fn [{:as args
                                                   :keys [hana/stat]}]
                                               (-> args
                                                   stat
                                                   dataset->csv))
                                    :DFMT {:type "csv"}}})
             {:hana/data (toydata/iris-ds)
              :hana/stat (comp #(tc/head % 20) :hana/data)
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
       {::ht/defaults {:VALDATA (fn [{:as args
                                      :keys [hana/stat]}]
                                  (-> args
                                      stat
                                      dataset->csv))
                       :DFMT {:type "csv"}
                       :X "sepal_width"
                       :Y "sepal_length"
                       :SIZE 100
                       :hana/stat (comp #(tc/head % 20) deref :hana/data)
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
       {::ht/defaults {:VALDATA (fn [{:as args
                                      :keys [hana/stat]}]
                                  (-> args
                                      stat
                                      dataset->csv))
                       :DFMT {:type "csv"}
                       :X "sepal_width"
                       :Y "sepal_length"
                       :SIZE 100
                       :hana/stat (comp #(tc/head % 20) deref :hana/data)
                       :hana/data (->WrappedValue (toydata/iris-ds))}})
      (update-data tc/head 5)
      hc/xform
      kind/vega-lite))



(defn svg-rendered [vega-lite-template]
  (assoc vega-lite-template
         :usermeta
         {:embedOptions {:renderer :svg}}))

(def view-base (svg-rendered ht/view-base))
(def point-chart (svg-rendered ht/point-chart))
(def line-chart (svg-rendered ht/line-chart))
(def layer-chart (svg-rendered ht/layer-chart))
(def point-layer ht/point-layer)
(def line-layer ht/line-layer)


(defn make-vega-lite [template]
  (->> template
       hc/xform
       kind/vega-lite))

(defn valdata-from-dataset [{:as args
                             :keys [hana/data
                                    hana/stat]}]
  (dataset->csv
   (if stat
     (stat args)
     @data)))

(defn base
  ([dataset-or-template]
   (base dataset-or-template {}))
  ([dataset-or-template subs]
   (if (tc/dataset? dataset-or-template)
     ;; a dataest
     (base dataset-or-template
           view-base
           subs)
     ;; a template
     (-> dataset-or-template
         (update ::ht/defaults merge subs)
         (assoc :kindly/f #'make-vega-lite)
         kind/fn)))
  ([dataset template subs]
   (-> template
       (update ::ht/defaults merge {:VALDATA valdata-from-dataset
                                    :DFMT {:type "csv"}
                                    :hana/data (->WrappedValue dataset)})
       (base subs))))


(defn plot [& args]
  (->> args
       (apply base)
       make))

(delay
  (-> (toydata/iris-ds)
      (base point-chart
            {:X :sepal_width
             :Y :sepal_length
             :MSIZE 200})))


(delay
  (-> (toydata/iris-ds)
      (plot point-chart
            {:X :sepal_width
             :Y :sepal_length
             :MSIZE 200})))


(defn layer
  ([context template args]
   (if (tc/dataset? context)
     (layer (base context {})
            template
            args)
     ;; else - the context is already a template
     (-> context
         (merge ht/layer-chart)
         (update-in [::ht/defaults :LAYER]
                    (comp vec conj)
                    (assoc template
                           ::ht/defaults (merge {:VALDATA valdata-from-dataset
                                                 :DFMT {:type "csv"}}
                                                args)))))))


(delay
  (-> (toydata/iris-ds)
      (base {:TITLE "dummy"
             :MCOLOR "green"
             :X :sepal_width
             :Y :sepal_length})
      (layer point-layer
             {:MSIZE 100})
      (layer line-layer
             {:MSIZE 4
              :MCOLOR "brown"})
      (update-data tc/random 20)))



(delay
  (-> (toydata/iris-ds)
      (base {:TITLE "dummy"
             :MCOLOR "green"
             :X :sepal_width

             :Y :sepal_length})
      (layer point-layer
             {:MSIZE 100})
      (layer line-layer
             {:MSIZE 4
              :MCOLOR "brown"})
      (update-data tc/random 20)
      plot
      (assoc :background "lightgrey")))


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
      (base {:TITLE "dummy"
             :MCOLOR "green"})
      (layer-point {:X :sepal_width
                    :Y :sepal_length
                    :MSIZE 100})
      (layer-line {:X :sepal_width
                   :Y :sepal_length
                   :MSIZE 4
                   :MCOLOR "brown"})))

(def smooth-stat
  (fn [{:as args
        :keys [hana/data]}]
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
                                 (tc/add-or-replace-column Y predictions-fn))))
          new-data (update-data-fn @data)]
      new-data)))


(defn layer-smooth
  ([context]
   (layer-smooth context {}))
  ([context args]
   (layer context
          line-chart
          (assoc args
                 :hana/stat smooth-stat))))


(delay
  (-> (toydata/iris-ds)
      (tc/select-columns [:sepal_width :sepal_length])
      (base {:X :sepal_width
             :Y :sepal_length})
      layer-point
      layer-smooth))

(delay
  (-> (toydata/iris-ds)
      (base {:X :sepal_width
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
      (base {:X :sepal_width
             :Y :sepal_length
             :COLOR "relative-time"})
      layer-point
      layer-smooth))


(delay
  (-> (toydata/iris-ds)
      (base {:X :sepal_width
             :Y :sepal_length})
      layer-point
      (layer-smooth {:X-predictors [:petal_width
                                    :petal_length]})))
