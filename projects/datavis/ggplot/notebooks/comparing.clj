(load-file "../../../header.edn")

;; # Comparing the data representations of ggplot plots

(ns comparing
  (:require [clojisr.v1.r :as r :refer [r r$ r->clj]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.walk :as walk]
            [tablecloth.api :as tc]
            [editscript.core :as editscript]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [representing]))

;; ## Exlploring a few plots

;; Let us explore and compare a few plots through their Clojure representations:

(defn h3 [title] (kind/hiccup [:h3 title]))
(defn h4 [title] (kind/hiccup [:h4 title]))

(defn ggplot-summary
  ([title r-code]
   (ggplot-summary r-code))
  ([title r-code prev-clj-to-compare]
   (let [plot (r r-code)
         clj (-> plot
                 representing/ggplot->clj
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
