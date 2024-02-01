(load-file "../../header.edn")

;; # Exploring ggplot

(ns ggplot
  (:require [clojisr.v1.r :as r :refer [r]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]))

(r/library "ggplot2")

(def plot
  (r "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\")
         + facet_wrap(~cyl))"))

(-> plot
    plotting/plot->svg
    kind/html)

(-> plot
    r/r->clj)
