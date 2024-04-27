(ns combining-tablecloth-and-hanami
  (:require [scicloj.kindly.v4.kind :as kind]
            [scicloj.noj.v1.paths :as paths]
            [scicloj.tempfiles.api :as tempfiles]
            [tablecloth.api :as tc]
            [tech.v3.dataset :as ds]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.metamorph.ml.toydata :as toydata]))

(defn invoke [[f arg]]
  (f arg))

(defn prepare-data [dataset]
  (when dataset
    (let [{:keys [path _]}
          (tempfiles/tempfile! ".csv")]
      (-> dataset
          (ds/write! path))
      {:values (slurp path)
       :format {:type "csv"}})))

(defn safe-update [m k f]
  (if (m k)
    (update m k f)
    m))

(defn layered-xform [{:keys [template args]}]
  (-> template
      (safe-update :layer
                   (partial
                    mapv
                    (fn [layer]
                      (-> layer
                          (update :template
                                  (partial merge (dissoc template
                                                         :layer
                                                         :metamorph/data)))
                          (update :args
                                  (partial merge args))
                          layered-xform))))
      (update :metamorph/data prepare-data)
      (hc/xform args)
      (update-keys #(case % :metamorph/data :data %))
      kind/vega-lite))

(defn svg-rendered [vega-lite-template]
  (assoc vega-lite-template
         :usermeta
         {:embedOptions {:renderer :svg}}))

(def point-chart
  (svg-rendered ht/point-chart))

(def line-chart
  (svg-rendered ht/line-chart))


(defn plot
  ([dataset template args]
   (plot (assoc template
                :metamorph/data dataset)
         args))
  ([template args]
   (let [dataset (:metamorph/data template)]
     (if (tc/grouped? dataset)
       (-> dataset
           (tc/aggregate {:plot (fn [group-dataset]
                                  [(plot (assoc template
                                                :metamorph/data group-dataset)
                                         args)])})
           (tc/rename-columns {:plot-0 :plot})
           kind/table)
       ;; else
       (kind/fn [layered-xform
                 {:template template
                  :args args}])))))


(defn layer
  ([context template args]
   (if (tc/dataset? context)
     (layer (plot context {} {})
            template
            args)
     (-> context
         (update
          1
          update
          :template
          update
          :layer
          (comp vec conj)
          {:template template
           :args args})))))


(-> (toydata/iris-ds)
    (plot point-chart
          {:X :sepal_width
           :Y :sepal_length}))

(-> (toydata/iris-ds)
    (plot {}
          {:TITLE "dummy"
           :MCOLOR "green"})
    (layer point-chart
           {:X :sepal_width
            :Y :sepal_length
            :MSIZE 100})
    (layer line-chart
           {:X :sepal_width
            :Y :sepal_length
            :MSIZE 4
            :MCOLOR "brown"}))
