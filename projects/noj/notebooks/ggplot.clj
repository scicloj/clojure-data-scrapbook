(load-file "../../header.edn")

;; # Exploring ggplot

;; following
;; [cxplot](https://cxplot.com/index.html)'s internal ggplot.as.list
;; in representing a plot as a data structure.

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

(r/require-r '[base])

(defn ->clj
  ([r-obj avoid]
   (->clj r-obj avoid []))
  ([r-obj avoid path]
   (prn path)
   (or (some-> (some->> r-obj
                        r.base/names
                        r->clj
                        (filter (complement avoid))
                        (map (fn [nam]
                               [(keyword nam) (-> r-obj
                                                  (r$ nam)
                                                  (->clj avoid
                                                         (conj path nam)))]))
                        (into {})))
       (if (-> r-obj
               r.base/class
               r->clj
               first
               (= "ggproto_method"))
         :ggproto-method)
       (if (-> r-obj
               r.base/is-list
               r->clj
               first)
         (-> r-obj
             r.base/length
             r->clj
             first
             range
             (->> (mapv (fn [i]
                          (prn [path (inc i)])
                          (-> r-obj
                              (r/brabra (inc i))
                              (->clj avoid
                                     (conj path [i])))))))
         r-obj))))

(-> plot
    (->clj #{"data" "plot_env"}))
