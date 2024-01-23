(load-file "../../header.edn")

;; # Machine learning - DRAFT

(ns ml
  (:require [scicloj.ml.core :as ml]
            [scicloj.ml.metamorph :as mm]
            [scicloj.ml.dataset :refer [dataset add-column]]
            [scicloj.ml.dataset :as ds]
            [fastmath.stats]
            [tablecloth.api :as tc]
            [scicloj.noj.v1.datasets :as datasets]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Linear regression

;; We will explore the Iris dataset:
(tc/head datasets/iris)


;; A Metamorph pipeline for linear regression:
(def additive-pipeline
  (ml/pipeline
   (mm/set-inference-target :sepal-length)
   (mm/drop-columns [:species])
   {:metamorph/id :model}
   (mm/model {:model-type :smile.regression/ordinary-least-square})))

;; Training and evaluating the pipeline on various subsets:
(def evaluations
  (ml/evaluate-pipelines
   [additive-pipeline]
   (ds/split->seq datasets/iris :holdout)
   ml/rmse
   :loss
   {:other-metrices [{:name :r2
                      :metric-fn fastmath.stats/r2-determination}]}))

;; Printing one of the trained models (note that the Smile regression model is recognized by Kindly and printed correctly):
(-> evaluations
    flatten
    first
    :fit-ctx
    :model
    ml/thaw-model)
