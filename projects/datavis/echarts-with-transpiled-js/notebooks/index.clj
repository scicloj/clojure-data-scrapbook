(load-file "../../../header.edn")

;; ---------------

;; |||
;; |-|-|
;; |Authors|Chriz Zheng, Daniel Slutsky|
;; |Initial version|2025-01-31|
;; |Last update|2025-01-31|

;; ---------------

;; Very often, data visualization with [Apache Echarts](https://echarts.apache.org/en/index.html)
;; can be done with nothing more than a JSON data structure.

;; This can be conveniently created from plain Clojure data structures,
;; and is supported by the [Kindly](https://scicloj.github.io/kindly/) standard.
;; See, for example, the *(link might change)*
;; [Data Visualizations with Echarts](https://scicloj.github.io/noj/noj_book.echarts.html)
;; tutorial at the Noj book.

;; However, sometimes a little bit of Javascript is necessary to define
;; **custom functions** to be used in Echarts.
;; For example, a function to determine the symbol size in a scatterplot.

;; One way to achieve that in Clojure is by **transpiling Clojure forms**
;; into Javascript. This can be done using
;; [std.lang](https://clojureverse.org/t/std-lang-a-universal-template-transpiler/),
;; a universal transpiler from Clojure to many languages.

;; In this tutorial, will demonstrate that by mimicking Echarts'
;; [life expectency timeline example](https://echarts.apache.org/examples/en/editor.html?c=scatter-life-expectancy-timeline)
;; inspired by the famous [Gapminder](https://en.wikipedia.org/wiki/Gapminder_Foundation)
;; demo by [Hans Rosling](https://en.wikipedia.org/wiki/Hans_Rosling).

^{:kindly/kind :kind/video
  :kindly/hide-code true}
{:youtube-id "hVimVzgtD6w"}

;; ## Goal

;; We wish to propose the idea of using `std.lang` transpilation
;; in certain visualization kinds of the Kindly standard.

;; This notebook can serve as a self-contained example to support the discussion.

;; ## Setup

;; We use [Tablecloth](https://scicloj.github.io/tablecloth) for data processing,
;; [Kindly](https://scicloj.github.io/kindly/) for annotating visualizations,
;; and most importantly, [std.lang](https://clojureverse.org/t/std-lang-a-universal-template-transpiler/)
;; for transpiling Clojure forms into Javascript.

(ns index
  (:require [scicloj.kindly.v4.kind :as kind]
            [std.lang :as l]
            [std.lib :as h]
            [tablecloth.api :as tc]))

;; ## Data

;; The Echarts tutorial above uses
;; [a pre-tailored dataset](https://echarts.apache.org/examples/data/asset/data/life-expectancy.json)
;; to make the visualization easy.

;; Here, we prefer starting from scratch with an official dataset.

;; We will use the data that powers the chart
;; ["Life expectancy vs. GDP per capita"](https://ourworldindata.org/grapher/life-expectancy-vs-gdp-per-capita?v=1&csvType=full&useColumnShortNames=false)
;; on the [Our World in Data](https://ourworldindata.org/) website.

(def raw-data
  (-> "data/life-expectancy-vs-gdp-per-capita.csv.gz"
      tc/dataset
      (tc/rename-columns {"Entity" :entity
                          "Year" :year
                          "Period life expectancy at birth - Sex: total - Age: 0" :life-expectency
                          "GDP per capita" :gdp-per-capita
                          "Population (historical)" :population})
      (tc/select-columns [:entity :year :life-expectency :gdp-per-capita :population])
      (tc/select-rows (fn [{:keys [entity year]}]
                        (>= year 1950)))))


;; As in the Echarts example, we will focus on the following countries:

(def countries
  #{"China","United States","United Kingdom","Russia","India","France","Germany","Australia","Canada","Cuba","Finland","Iceland","Japan","North Korea","South Korea","New Zealand","Norway","Poland","Turkey"})

;; ## Transpiling to Javascript 

;; We will use `std.lang` through the following convenience function.
;; This form of usage is handy in our case, but may need more thinking
;; before genearlizing.

(defn js
  "Transpile the given Clojure `forms` to Javascript code
  to be run inside a closure."
  [& forms]
  ((l/ptr :js)
   (h/$ ((:- \(
             (fn []
               ~@forms)
             \))))))

;; For example:

(kind/code
 (js '(var x 9)
     '(+ x 11)))

;; ## Generating echarts plots

;; The following function will allow us to generate Ecahrts
;; plots with transpiled Javascript.

(defn echarts
  "Given some `data` and a Clojure `form`, transpile both of them
  to Javascript and return a Hiccup block of a data visualization.
  The transpiled `form` is used as the Echarts specification, that
  is a data structure which may contain functions if necessary.
  The transpiled `data` is kept in a Javascript variable `data`,
  which can be referred to from the Echarts specification."
  [data form]
  (kind/hiccup
   [:div
    {:style {:height "400px"
             :width "100%"}}
    [:script
     (js (list 'var 'data data)
         '(var myChart
               (echarts.init document.currentScript.parentElement))
         (list 'myChart.setOption form))]]
   {:html/deps [:echarts]}))

;; For example, here is a basic scatterplot.
;; Note how we refer to the data from the plot specification.

(-> raw-data
    (tc/select-rows #(and (-> % :year (= 1990))
                          (-> % :entity countries)))
    (tc/select-columns [:gdp-per-capita
                        :life-expectency
                        :population
                        :entity])
    tc/drop-missing
    tc/rows
    (echarts
     {:tooltip {}
      :xAxis {:type "log"}
      :yAxis {}
      :series [{:type "scatter"
                :data 'data}]}))

;; We may make the scatterplot more informative by using
;; the symbol size and colour.
;; Note how we define the symbol size as a Javascript function.

(-> raw-data
    (tc/select-rows #(and (-> % :year (= 1990))
                          (-> % :entity countries)))
    (tc/select-columns [:gdp-per-capita
                        :life-expectency
                        :population
                        :entity])
    tc/drop-missing
    tc/rows
    (echarts
     {:tooltip {}
      :xAxis {:type "log"}
      :yAxis {}
      :visualMap [{:show false
                   :dimension 3
                   :categories (vec countries)
                   :inRange {:color
                             ;; Here we are following the practice of the
                             ;; original Echarts example in duplicating
                             ;; the list of colours.
                             ;; We do not understand why this is necessary
                             ;; yet.
                             (vec
                              (#(concat % %)
                               ["#51689b", "#ce5c5c", "#fbc357", "#8fbf8f", "#659d84", "#fb8e6a", "#c77288", "#786090", "#91c4c5", "#6890ba"]))}}]
      :series [{:type "scatter"
                :data 'data
                :symbolSize '(fn [data]
                               (-> data
                                   (. [2])
                                   Math.sqrt
                                   (/ 500)
                                   return))}]}))


;; ## The Gapminder example
;; Now let us create a simple version of the Gapminder animation.
;; Note that the animation itself can be specified using plain data.
;; The transpiled fuctions were necessary just for little details
;; such as tooltip and symbol size.

(let [data-by-year (-> raw-data
                       (tc/select-rows #(and (-> % :entity countries)
                                             (-> % :year (>= 1990))))
                       (tc/map-columns :year [:year] str)
                       (tc/select-columns [:gdp-per-capita
                                           :life-expectency
                                           :population
                                           :entity
                                           :year])
                       tc/drop-missing
                       (tc/group-by :year {:result-type :as-map})
                       (->> (into (sorted-map))))]
  (-> data-by-year
      (update-vals tc/rows)
      (echarts
       {:timeline {:autoPlay true
                   :orient "vertical"
                   :symbol "none"
                   :playInterval 1000
                   :left nil :rifht 0 :top 20 :bottom 20
                   :width 44 :height nil
                   :data (vec (keys data-by-year))}
        :tooltip {:formatter '(fn [obj]
                                (-> obj
                                    (. value)
                                    (. [3])
                                    return))}
        :xAxis {:name "GDP per capita"
                :nameGap 25
                :nameLocation "middle"
                :axisLabel {:formatter "${value}"}
                :nameTextStyle {:fontSize 18}
                :type "log"
                :min 300
                :max 100000}
        :yAxis {:name "life expectency"
                :nameGap 25
                :nameLocation "middle"
                :nameTextStyle {:fontSize 18}
                :min 0
                :max 80}
        :visualMap [{:show false
                     :dimension 3
                     :categories (vec countries)
                     :inRange {:color
                               ;; Here we are following the practice of the
                               ;; original Echarts example in duplicating
                               ;; the list of colours.
                               ;; We do not understand why this is necessary
                               ;; yet.
                               (vec
                                (#(concat % %)
                                 ["#51689b", "#ce5c5c", "#fbc357", "#8fbf8f", "#659d84", "#fb8e6a", "#c77288", "#786090", "#91c4c5", "#6890ba"]))}}]
        :options (->> data-by-year
                      keys
                      (mapv
                       (fn [year]
                         {:series [{:type "scatter"
                                    :data (list '. 'data [(str year)])
                                    :symbolSize '(fn [data]
                                                   (-> data
                                                       (. [2])
                                                       Math.sqrt
                                                       (/ 500)
                                                       return))}]})))})))

;; ## Epilogue

;; The practice we demonstrated here can potenially be handy in a few
;; different situations where we need to write a little Javascript from
;; within a Clojure namespace.

;; We also hope to explore other cases where `std.lang` could be helpful
;; in interactivg with other languages. Note that it provides not only
;; a transpiler but also mutliple ways to connect to runtimes, which we haven't
;; used here.

;; We will keep discussing these directions at the
;; [#kindly-dev channel](https://clojurians.zulipchat.com/#narrow/channel/454856-kindly-dev/)
;; of the [Clojurians Zulip chat](https://scicloj.github.io/docs/community/chat/).


