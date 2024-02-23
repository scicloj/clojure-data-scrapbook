(load-file "../../../header.edn")

;; # Using ggplot Clojure using ClojisR

;; ## Setup

;; We will use [ClojisR](https://scicloj.github.io/clojisr/) to call R from Clojure.

(ns using-clojisr
  (:require [clojisr.v1.r :as r :refer [r]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]))

;; ## Plotting by calling R code

;; Let us load ggplot2 at the R runtime:
(r/library "ggplot2")

;; We will use the `mpg` dataset, which is part of the `ggplot` package.
(-> "mpg"
    r
    r/r->clj)

;; Here is how we may generate a plot from R code:
(def plot1
  (r "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\")
         + facet_wrap(~cyl))"))

;; Rendering as SVG:
(-> plot1
    plotting/plot->svg
    kind/html)

;; Rendering as a bitmap image:
(-> plot1
    plotting/plot->buffered-image)

;; ## Plotting by calling generated R code

;; We may also plot by [generating](https://scicloj.github.io/clojisr/doc/clojisr/v1/codegen-test/) the R code from a Clojure form.

;; Let us require ggplot as a Clojure namespace.
(r/require-r '[ggplot2 :as gg])

;; Now we can plot using code generation:
(-> (r/r+ (gg/ggplot gg/mpg
                     (gg/aes :x 'cty
                             :y 'hwy
                             :color '(factor cyl)))
          (gg/geom_point)
          (gg/stat_smooth :method "lm")
          (gg/facet_wrap '(tilde . cyl)))
    r
    plotting/plot->svg
    kind/html)

;; ## Plotting by calling generated R code with Clojure data

;; We may further use ClojisR's code generation
;; with Clojure data.

;; Let us read the same `mpg` dataset. We have it in the metamorph.ml package.
