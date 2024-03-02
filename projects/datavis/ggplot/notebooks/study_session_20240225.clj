(load-file "../../../header.edn")

(ns study-session-20240225
  (:require [clojisr.v1.r :as r :refer [r]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [scicloj.metamorph.ml.toydata :as toydata]
            [scicloj.metamorph.ml.toydata.ggplot :as toydata.ggplot]
            [representing]))

;; # Code examples from study session 2024-02-25

;; Add ggplot2 to the R runtime:
(r/library "ggplot2")

;; The `mpg` dataset:
(r "mpg")

;; Evaluating R and showing a plot as a bitmap image:
(plotting/plot->buffered-image
 (r "
(ggplot(mpg, aes(x = cty, y = hwy, color = year)) +
geom_point())
"))

;; Showing a plot as svg:
(plotting/plot->svg
 (r "
(ggplot(mpg, aes(x = cty, y = hwy, color = year)) +
geom_point())
"))

;; Use ggplot2 as a Clojure namespace:
(r/require-r '[ggplot2 :as gg])

;; The `mpg` dataset as a Clojure (tech.ml.dataset) dataset:
toydata.ggplot/mpg

;; Evaluating R code generated from the Clojure namespace,
;; with Clojure data.
(-> toydata.ggplot/mpg
    (gg/ggplot (gg/aes :x 'cty
                       :y 'hwy
                       :color 'year))
    (r/r+ (gg/geom_point))
    r
    plotting/plot->buffered-image)

;; Representing a ggplot plot as Clojure data:
(-> toydata.ggplot/mpg
    (gg/ggplot (gg/aes :x 'cty
                       :y 'hwy
                       :color 'year))
    (r/r+ (gg/geom_point))
    r
    representing/ggplot->clj
    ;; (dissoc the dataset itself
    ;; to keep the structure small)
    (dissoc :data))









;; Using log scale
;; (keeping axis labels,
;; changing the spacing):

(-> toydata.ggplot/mpg
    (gg/ggplot (gg/aes :x 'cty
                       :y 'hwy
                       :color 'year))
    (r/r+ (gg/geom_point :size 5)
          (gg/scale_y_log10))
    r
    plotting/plot->svg
    kind/html)
