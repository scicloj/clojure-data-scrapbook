(load-file "../../../header.edn")

;; # Representing ggplot plots as Clojure data

;; ## Setup

;; We will use [ClojisR](https://scicloj.github.io/clojisr/) to call R from Clojure.

(ns repesenting
  (:require [clojisr.v1.r :as r :refer [r r$ r->clj]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.walk :as walk]
            [tablecloth.api :as tc]
            [editscript.core :as editscript]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

;; Loading ggplot2:
(r/library "ggplot2")

;; Requireing the base R library as a Clojure namespace:
(r/require-r '[base])

;; ## An example plot

;; Here is how we may generate and display a plot:

(-> "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\")
         + facet_wrap(~cyl))"
    r
    plotting/plot->svg
    kind/html)

;; We may also [generate](https://scicloj.github.io/clojisr/doc/clojisr/v1/codegen-test/) the R code from a Clojure form and pass Clojure data to R, but for now, we are simply using R code using an R dataset.

;; We are using the `mpg` dataset, which is part of the `ggplot` package.

(-> "mpg"
    r
    r->clj)

;; ## Representing plots as Clojure data

;; A ggplot object is an R list of [ggproto](https://bookdown.dongzhuoer.com/hadley/ggplot2-book/introducing-ggproto) objects. We recursively unwrap this structure and convert it to Clojure.


(defn ggplot->clj
  ([r-obj]
   (ggplot->clj r-obj {} []))
  ([r-obj
    {:as options
     :keys [avoid]
     :or {avoid #{"plot_env"}}}
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
                                                           (ggplot->clj options
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
                        (-> r-obj
                            (r/brabra (inc i))
                            (ggplot->clj options
                                         (conj path [i])))))))
       ;;
       (r.base/is-atomic r-obj) (try (r->clj r-obj)
                                     (catch Exception e
                                       (-> r-obj println with-out-str)))
       :else r-obj))))

;; In the conversion, we avoid some big parts of the structure, e.g., the `"plot_env"` member. We also do no report the toplevel `"data"` member, which is simply the dataset.

;; For example:

(-> "(ggplot(mpg, aes(cty, hwy))
         + geom_point())"
    r
    ggplot->clj
    (dissoc :data))


;; ## Exlploring a few plots

;; Let us explore and compare a few plots this way:

(defn h3 [title] (kind/hiccup [:h3 title]))
(defn h4 [title] (kind/hiccup [:h4 title]))

(defn ggplot-summary
  ([title r-code]
   (ggplot-summary r-code))
  ([title r-code prev-clj-to-compare]
   (let [plot (r r-code)
         clj (-> plot
                 ggplot->clj
                 (dissoc :data))]
     {:title title
      :r-code r-code
      :image (plotting/plot->buffered-image plot)
      :clj clj
      :diff (when prev-clj-to-compare
              (-> prev-clj-to-compare
                  (editscript/diff clj)
                  pp/pprint
                  with-out-str
                  (str/replace #": " ":_ ")
                  read-string))})))

(defn view-summary [{:keys [title r-code image clj diff]}]
  (kind/fragment
   [(h3 title)
    (h4 "R code")
    (kind/md
     (format "\n```\n%s\n```\n"
             r-code))
    (h4 "plot")
    image
    (h4 "clj data")
    clj
    (when diff
      (kind/fragment
       [(h4 "clj diff with previous")
        diff]))]))

(->> [;;
      ["A scatterplot"
       "(ggplot(mpg, aes(cty, hwy))
         + geom_point())"]
      ;;
      ["A scatterplot with colours"
       "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point())"]
      ;;
      ["A scatterplot with colours and smoothing"
       "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\"))"]
      ;;
      ["A scatterplot with colours, smoothing, and facets"
       "(ggplot(mpg, aes(cty, hwy, color=factor(cyl)))
         + geom_point()
         + stat_smooth(method=\"lm\")
         + facet_wrap(~cyl))"]]
     (reductions (fn [prev-summary [title r-code]]
                   (ggplot-summary title
                                   r-code
                                   (:clj prev-summary)))
                 nil)
     rest
     (map view-summary)
     kind/fragment)


;; ## Exploring

(-> "(ggplot(mpg, aes(cty, hwy)))"
    r
    ggplot->clj
    (dissoc :data))


(-> "(ggplot(mpg, aes(cty, hwy)))"
    r
    ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy)))"
    r
    ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=cyl), size=5)
      + ylim(c(20,30)))"
    r
    ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=factor(cyl)), size=20))"
    r
    plotting/plot->buffered-image)

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=cyl), size=5)
      + ylim(c(20,30))
      + scale_x_log10())"
    r
    ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=cyl), size=5)
      + ylim(c(20,30))
      + scale_x_log10()
      + theme_linedraw())"
    r
    ggplot->clj
    (dissoc :data))

(-> "(ggplot(data.frame(x = rnorm(1000, 2, 2)), aes(x)) +
      geom_histogram(aes(y=..density..)) +  # scale histogram y
      geom_density(col = 'red', size=5))"
    r
    plotting/plot->buffered-image)

(-> "(ggplot(data.frame(x = rnorm(1000, 2, 2)), aes(x)) +
      geom_histogram(aes(y=..density..)) +  # scale histogram y
      geom_density(col = 'red'))"
    r
    ggplot->clj
    (dissoc :data))
