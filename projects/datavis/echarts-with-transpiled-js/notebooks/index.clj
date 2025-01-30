;; # Creating Echarts Plots with Javascript transpiled using `std.lang` - DRAFT

;; We will mimic Echarts' [example](https://echarts.apache.org/examples/en/editor.html?c=scatter-life-expectancy-timeline)
;; inspired by the famous [Gapminder](https://en.wikipedia.org/wiki/Gapminder_Foundation)
;; demo by [Hans Rosling](https://en.wikipedia.org/wiki/Hans_Rosling).

^{:kindly/kind kind/video
  :kindly/hide-code true}
{:youtube-id "hVimVzgtD6w"}

;; ## Setup

;; We use [Tablecloth](https://scicloj.github.io/tablecloth) for data processing,
;; [Kindly](https://scicloj.github.io/kindly/) for annotating visualizations,
;; and most importantly, [std.lang](https://clojureverse.org/t/std-lang-a-universal-template-transpiler/)
;; for transpiling Clojure forms into Javascript.

(ns index
  (:require [scicloj.kindly.v4.kind :as kind]
            [std.lang :as l]
            [tablecloth.api :as tc]))

;; ## Data

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


;; ## Transpiling JS

;; We will use this convenience function:

(defn js [& forms]
  ((l/ptr :js)
   (cons 'do forms)))

;; For example:

(js '(var x 9)
    '(+ x 11))

;; We will generate echarts plots using 

(defn echarts [data form]
  (kind/hiccup
   [:div
    {:style {:height "800px"
             :width "100%"}}
    [:script
     (js (list 'var 'data data)
         '(var myChart
               (echarts.init document.currentScript.parentElement))
         (list 'myChart.setOption form))]]
   {:html/deps [:echarts]}))

(def countries
  #{"China","United States","United Kingdom","Russia","India","France","Germany","Australia","Canada","Cuba","Finland","Iceland","Japan","North Korea","South Korea","New Zealand","Norway","Poland","Turkey"})


(let [data (-> raw-data
               (tc/select-rows #(and (-> % :year (= 1990))
                                     (-> % :entity countries)))
               (tc/select-columns [:gdp-per-capita
                                   :life-expectency
                                   :population
                                   :entity])
               tc/drop-missing)]
  (echarts (tc/rows data)
           {:tooltip {}
            :xAxis {:type "log"}
            :yAxis {}
            :visualMap [{:show false
                         :dimension 3
                         :categories (vec (distinct (:entity data)))
                         :inRange {:color
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


(let [data-by-year (-> raw-data
                       (tc/select-rows #(and (-> % :entity countries)
                                             (-> % :year (>= 1970))))
                       (tc/map-columns :year [:year] str)
                       (tc/select-columns [:gdp-per-capita
                                           :life-expectency
                                           :population
                                           :entity
                                           :year])
                       tc/drop-missing
                       (tc/group-by :year {:result-type :as-map})
                       (->> (into (sorted-map))))]
  (echarts (-> data-by-year
               (update-vals tc/rows))
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
                         :categories (->> data-by-year
                                          vals
                                          first
                                          :entity
                                          distinct
                                          sort
                                          (mapv str))
                         :inRange {:color
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
                                                           return))}]})))}))
