(load-file "../../header.edn")

;; # Exploring ggplot

(ns ggplot
  (:require [clojisr.v1.r :as r :refer [r r$ r->clj]]
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
    r->clj
    (dissoc :data))


;; Trying to mimic [cxplot](https://cxplot.com/index.html)'s internal ggplot.as.list (WIP):
(defn gg-facet [ggplot]
  (let [f (-> ggplot
              (r$ 'facet)
              (r$ 'params)
              (r$ 'facets))]
    (when (-> `(is.null ~f)
              r
              r->clj)
      (let [facet (r `(ls ~f))]
        {:facet facet
         :facet-levels (-> plot
                           (r$ 'data)
                           (r/bra facet))}))))

(-> plot
    gg-facet
    (update-vals r->clj))
