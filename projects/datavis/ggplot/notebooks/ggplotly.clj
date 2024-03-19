(load-file "../../../header.edn")

(ns ggplotly
  (:require [scicloj.kindly.v4.kind :as kind]
            [charred.api :as charred]
            [clojure.math :as math]
            [scicloj.ggclj.geom.point :as point]
            [scicloj.ggclj.api :as gg]
            [scicloj.metamorph.ml.toydata.ggplot :as toydata.ggplot]
            [tablecloth.api :as tc]))

;; # Rendering ggclj using htmlwidgets ggplotly

;; Here we render plots through the JS client side of [ggplotly](https://plotly.com/ggplot2/)
;; - an R package (part of the [htmlwidgets](https://www.htmlwidgets.org/) ecosystem) offering a Plotly fronted for ggplot2's grammar of graphics implementation.

;; The following is a work-in-progress attempt to generate
;; JSON specs of the kind consumed by ggplotly's client side.

;; The following spec function was generaged by mimicking R's
;; `ggplotlTentativey(ggplot(mtcars, aes(wt, mpg)) + geom_point())`'s output.
;; Therefore, some parts are hard-coded and require generalization.
;; Other parts are still missing (e.g., specifying colours).

(defn ->ggplotly-spec [{:keys [layers labels]}]
  (kind/htmlwidgets-ggplotly
   (let [;; assuming one layer:
         {:keys [data xmin xmax ymin ymax]} (first layers)
         ;; an auxiliary function:
         ->tickvals (fn [l r]
                      (let [jump (-> (- r l)
                                     (/ 6)
                                     math/floor
                                     int
                                     (max 1))]
                        (-> l
                            math/ceil
                            (range r jump))))]
     {:x
      {:config
       {:doubleClick "reset",
        :modeBarButtonsToAdd ["hoverclosest" "hovercompare"],
        :showSendToCloud false},
       :layout
       {:plot_bgcolor "rgba(235,235,235,1)",
        :paper_bgcolor "rgba(255,255,255,1)",
        :legend
        {:bgcolor "rgba(255,255,255,1)",
         :bordercolor "transparent",
         :borderwidth 1.88976377952756,
         :font {:color "rgba(0,0,0,1)", :family "", :size 11.689497716895}},
        :xaxis (let [tickvals (->tickvals xmin xmax)
                     ticktext (mapv str tickvals)
                     range-len (- xmax xmin)
                     range-expansion (* 0.1 range-len)
                     expanded-range [(- xmin range-expansion)
                                     (+ xmax range-expansion)]]
                 {:linewidth 0,
                  :nticks nil,
                  :linecolor nil,
                  :ticklen 3.65296803652968,
                  :tickcolor "rgba(51,51,51,1)",
                  :tickmode "array",
                  :gridcolor "rgba(255,255,255,1)",
                  :automargin true,
                  :type "linear",
                  :tickvals tickvals
                  :zeroline false,
                  :title
                  {:text (:x labels),
                   :font {:color "rgba(0,0,0,1)", :family "", :size 14.6118721461187}},
                  :tickfont {:color "rgba(77,77,77,1)", :family "", :size 11.689497716895},
                  :autorange false,
                  :showticklabels true,
                  :showline false,
                  :showgrid true,
                  :ticktext ticktext
                  :ticks "outside",
                  :gridwidth 0.66417600664176,
                  :anchor "y",
                  :domain [0 1],
                  :hoverformat ".2f",
                  :tickangle 0,
                  :tickwidth 0.66417600664176,
                  :categoryarray ticktext,
                  :categoryorder "array",
                  :range expanded-range},)
        :font {:color "rgba(0,0,0,1)", :family "", :size 14.6118721461187},
        :showlegend false,
        :barmode "relative",
        :yaxis (let [tickvals (->tickvals ymin ymax)
                     ticktext (mapv str tickvals)
                     range-len (- ymax ymin)
                     range-expansion (* 0.1 range-len)
                     expanded-range [(- ymin range-expansion)
                                     (+ ymax range-expansion)]]
                 {:linewidth 0,
                  :nticks nil,
                  :linecolor nil,
                  :ticklen 3.65296803652968,
                  :tickcolor "rgba(51,51,51,1)",
                  :tickmode "array",
                  :gridcolor "rgba(255,255,255,1)",
                  :automargin true,
                  :type "linear",
                  :tickvals tickvals,
                  :zeroline false,
                  :title
                  {:text (:y labels),
                   :font {:color "rgba(0,0,0,1)", :family "", :size 14.6118721461187}},
                  :tickfont {:color "rgba(77,77,77,1)", :family "", :size 11.689497716895},
                  :autorange false,
                  :showticklabels true,
                  :showline false,
                  :showgrid true,
                  :ticktext ticktext,
                  :ticks "outside",
                  :gridwidth 0.66417600664176,
                  :anchor "x",
                  :domain [0 1],
                  :hoverformat ".2f",
                  :tickangle 0,
                  :tickwidth 0.66417600664176,
                  :categoryarray ticktext,
                  :categoryorder "array",
                  :range expanded-range},)
        :hovermode "closest",
        :margin
        {:t 25.7412480974125,
         :r 7.30593607305936,
         :b 39.6955859969559,
         :l 37.2602739726027},
        :shapes
        [{:yref "paper",
          :fillcolor nil,
          :xref "paper",
          :y1 1,
          :type "rect",
          :line {:color nil, :width 0, :linetype []},
          :y0 0,
          :x1 1,
          :x0 0}]},
       :highlight
       {:on "plotly_click"
        :persistent false,
        :dynamic false,
        :selectize false,
        :opacityDim 0.2,
        :selected {:opacity 1},
        :debounce 0},
       :base_url "https://plot.ly",
       :cur_data "1f2fea5b54d146",
       :source "A",
       :shinyEvents
       ["plotly_hover"
        "plotly_click"
        "plotly_selected"
        "plotly_relayout"
        "plotly_brushed"
        "plotly_brushing"
        "plotly_clickannotation"
        "plotly_doubleclick"
        "plotly_deselect"
        "plotly_afterplot"
        "plotly_sunburstclick"],
       :attrs {:1f2fea5b54d146 {:x {}, :y {}, :type "scatter"}},
       :data
       [{:y (:y data)
         :hoveron "points",
         :frame nil,
         :hoverinfo "text",
         :marker
         {:autocolorscale false,
          :color "rgba(0,0,0,1)",
          :opacity 1,
          :size 5.66929133858268,
          :symbol "circle",
          :line {:width 1.88976377952756, :color "rgba(0,0,0,1)"}},
         :mode "markers",
         :type "scatter",
         :xaxis "x",
         :showlegend false,
         :yaxis "y",
         :x (:x data)
         :text (-> data
                   (tc/select-columns [:x :y])
                   (tc/rows :as-maps)
                   (->> (mapv pr-str)))}]},
      :evals [],
      :jsHooks []})))

;; Now here is how we can display a ggclj structure as a ggplotly plot.

(defn ggplotly [ggplot]
  (-> ggplot
      gg/ggplot-build
      ->ggplotly-spec))

;; Let us try it with ggplot's `mpg` dataset.

(-> toydata.ggplot/mpg
    (gg/ggplot (gg/aes :displ :hwy))
    (point/geom-point)
    ggplotly)
