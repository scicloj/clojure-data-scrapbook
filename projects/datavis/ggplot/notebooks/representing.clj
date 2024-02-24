(load-file "../../../header.edn")

;; # Representing ggplot plots as Clojure data

(ns representing
  (:require [clojisr.v1.r :as r :refer [r r$ r->clj]]
            [clojisr.v1.applications.plotting :as plotting]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.walk :as walk]
            [tablecloth.api :as tc]
            [editscript.core :as editscript]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(r/library "ggplot2")
(r/require-r '[base])

;; ## A representation function

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

;; ## An example

;; For example:

(delay
  (-> "(ggplot(mpg, aes(cty, hwy))
         + geom_point())"
      r
      ggplot->clj
      (dissoc :data)))
