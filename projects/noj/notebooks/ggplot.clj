(load-file "../../header.edn")

;; # Exploring ggplot

;; following
;; [cxplot](https://cxplot.com/index.html)'s internal ggplot.as.list
;; in representing a plot as a data structure.

(ns ggplot
  (:require [clojisr.v1.r :as r :refer [r r$ r->clj]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.walk :as walk]
            [tablecloth.api :as tc]))

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

(defn ggolot->clj
  ([r-obj
    options]
   (ggolot->clj r-obj options []))
  ([r-obj
    {:as options
     :keys [avoid]}
    path]
   #_(prn path)
   (let [relevant-names (some->> r-obj
                                 r.base/names
                                 r->clj
                                 (filter (complement avoid)))]
     (cond
       ;;
       ;; a named list or a ggproto object
       (seq relevant-names) (->> relevant-names
                                 (map (fn [nam]
                                        [(keyword nam) (-> r-obj
                                                           (r$ nam)
                                                           (ggolot->clj options
                                                                        (conj path nam)))]))
                                 (into {}))
       ;;
       ;; a ggproto method
       (-> r-obj
           r.base/class
           r->clj
           first
           (= "ggproto_method"))
       :ggproto-method
       ;;
       ;; an unnamed list
       (-> r-obj
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
                            (ggolot->clj options
                                         (conj path [i])))))))
       ;;
       (r.base/is-atomic r-obj) (try (r->clj r-obj)
                                     (catch Exception e
                                       (-> r-obj println with-out-str)))
       :else r-obj))))

(-> "(ggplot(mpg, aes(cty, hwy))
         + geom_point())"
    r
    (ggolot->clj {:avoid #{"data" "plot_env"}}))



(defn h4 [title]
  (kind/hiccup [:h3 title]))

(defn ggplot-summary [r-code]
  (let [plot (r r-code)
        clj (-> plot
                (ggolot->clj {:avoid #{"data" "plot_env"}})
                (->> (walk/postwalk (fn [form]
                                      (if (and (symbol? form)
                                               (-> form str (= "~")))
                                        (str form)
                                        form)))))]
    (kind/fragment
     [(h4 "code")
      (kind/md
       (format "\n```{r eval=FALSE}\n%s\n```\n"
               r-code))
      (h4 "image")
      (plotting/plot->buffered-image plot)
      (h4 "clj")
      (kind/pprint clj)])))

(ggplot-summary
 "(ggplot(mpg, aes(cty, hwy))
         + geom_point())")


;; ## Exlploring a few plots

;; ### A scatterplot

(ggplot-summary
 "(ggplot(mpg, aes(cty, hwy))
         + geom_point())")

;; ### A scatterplot with colours

(ggplot-summary
 "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point())")


;; ### A scatterplot with colours and smoothing

(ggplot-summary
 "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\"))")

;; ### A scatterplot with colours, smoothing, and facets

(ggplot-summary
 "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\")
         + facet_wrap(~cyl))")
