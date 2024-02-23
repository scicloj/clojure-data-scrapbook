(load-file "../../../header.edn")

;; # Using ggplot Clojure using ClojisR

;; ## Setup

;; We will use [ClojisR](https://scicloj.github.io/clojisr/) to call R from Clojure.

(ns using-clojisr
  (:require [clojisr.v1.r :as r :refer [r r$ r->clj]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]))

;; Loading ggplot2:
(r/library "ggplot2")

;; We will use the `mpg` dataset, which is part of the `ggplot` package.
(-> "mpg"
    r
    r->clj)

;; Here is how we may generate and display a plot:
(-> "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\")
         + facet_wrap(~cyl))"
    r
    plotting/plot->svg
    kind/html)

;; We may also [generate](https://scicloj.github.io/clojisr/doc/clojisr/v1/codegen-test/) the R code from a Clojure form and pass Clojure data to R, but for now, we are simply using R code using an R dataset.
