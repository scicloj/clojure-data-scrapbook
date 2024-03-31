^{:kindly/hide-code true
  :clay {:quarto {:title "Clojure Data Scrapbook"}}}

;; # Main analysis

(ns index
  (:require [clojure.string :as str]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

(-> [:img {:src "https://scicloj.github.io/sci-cloj-logo-transparent.png"
           :alt "SciCloj logo"
           :width "100"
           :align "right"}]
    kind/hiccup
    kindly/hide-code)

;; The Clojure Data Scrapbook is a collection of
;; community-contributed examples for the emerging Clojure data stack.
;;
;; You can interact with the examples by cloning this project and starting a REPL.
;;
;; This project is part of the [SciCloj community](https://scicloj.github.io/docs/community/about/).

;; ## Tutorials

(-> {:row-maps (->> [[:created "2023-12-16"
                      :updated nil
                      :title "Clay Calva integration - datavis demo"
                      :url "projects/visual-tools/clay-calva-demo-20231216/index.html"
                      :source-path "projects/visual-tools/clay-calva-demo-20231216"
                      :youtube-id "X_SsjhmG5Ok"
                      :tags [:visual-tools :clay :calva :noj
                             :datavis :hanami :tablecloth]]
                     [:created "2023-12-17"
                      :updated nil
                      :title "Clay CIDER integration - image processing demo"
                      :url "projects/visual-tools/clay-cider-demo-20231217/index.html"
                      :source-path "projects/visual-tools/clay-cider-demo-20231217"
                      :youtube-id "fd4kjlws6Ts"
                      :tags [:visual-tools :clay :cider :noj
                             :image-processing :dtype-next :tensors]]
                     [:created "2023-12-31"
                      :updated nil
                      :title "Reading HDF files"
                      :url "projects/data-formats/hdf/index.html"
                      :source-path "projects/data-formats/hdf/"
                      :youtube-id nil
                      :tags [:data-formats :hdf :dtype-next :tensors]]
                     [:created "2024-01-11"
                      :updated nil
                      :title "Machine learning - DRAFT"
                      :url "projects/noj/ml.html"
                      :source-path "projects/noj/notebooks/ml.clj"
                      :youtube-id nil
                      :tags [:noj :ml :scicloj.ml :draft]]
                     [:created "2024-01-25"
                      :updated nil
                      :title "Wolfram Lanauge interop with Wolframite"
                      :url "projects/math/wolframite/index.html"
                      :source-path "projects/math/wolframite/"
                      :youtube-id nil
                      :tags [:math :wolframite :interop]]
                     [:created "2024-02-06"
                      :updated "2024-03-19"
                      :title "Exploring ggplot"
                      :url "projects/datavis/ggplot/index.html"
                      :source-path "projects/datavis/ggplot/"
                      :youtube-id nil
                      :tags [:noj :r :clojisr :interop :ggplot :datavis]]
                     [:created "2024-02-07"
                      :updated nil
                      :title "Seattle parks & Neigborhoods - DRAFT"
                      :url "projects/geography/seattle-parks/index.html"
                      :source-path "projects/geography/seattle-parks"
                      :youtube-id nil
                      :tags [:geography :gis :tablecloth :datavis :draft]]
                     [:created "2024-03-02"
                      :updated nil
                      :title "Exploring Observable - DRAFT"
                      :url "projects/datavis/observable/index.html"
                      :source-path "projects/datavis/observable"
                      :youtube-id nil
                      :tags [:datavis :observable :dashboards :draft]]
                     [:created "2024-03-24"
                      :updated "2024-03-27"
                      :title "Chicago bike trips"
                      :url "projects/geography/chicago-bikes/index.html"
                      :source-path "projects/geography/chicago-bikes"
                      :youtube-id nil
                      :tags [:geography :gis :tablecloth :datavis :noj :hanami :vega-lite]]
                     [:created "2024-03-30"
                      :updated "2024-03-31"
                      :title "Noj getting started - from raw data to a blog post"
                      :url "https://scicloj.github.io/noj-getting-started"
                      :source-path "https://github.com/scicloj/noj-getting-started"
                      :youtube-id "5GluhUmMlpM"
                      :tags [:geography :gis :tablecloth :datavis :noj :hanami :vega-lite :clay :calva]]]
                    (map (fn [[created updated
                               title url source-path youtube-id tags]]
                           (let [draft (some #{:draft} tags)]
                             (mapv (if draft
                                     (fn [v]
                                       (kind/hiccup
                                        [:div {:style {:opacity 0.4}}
                                         v]))
                                     identity)
                                   [(kind/hiccup
                                     [:small created])
                                    (kind/hiccup
                                     [:small updated])
                                    (kind/hiccup
                                     [:small
                                      [:div {:style {:width "300px"}}
                                       [:p [:a {:href url}
                                            title]]
                                       [:p [:a {:style {:font-family "monospace"
                                                        :background-color "#fdf6e3"}
                                                :href (if (re-matches #"http.*" source-path)
                                                        source-path
                                                        (str "https://github.com/scicloj/clojure-data-scrapbook/tree/main/"
                                                             source-path))}
                                            "(source)"]]
                                       (when draft [:p "(draft)"])]])
                                    (when youtube-id
                                      (kind/video {:youtube-id youtube-id}))
                                    (->> tags
                                         (map name)
                                         (str/join ", "))]))))
                    reverse)
     :column-names ["created"
                    "updated"
                    "title"
                    "video"
                    "tags"]}
    (kind/table {:datatables {:paging false}})
    kindly/hide-code)

;; ## Contributing

;; (coming soon)
