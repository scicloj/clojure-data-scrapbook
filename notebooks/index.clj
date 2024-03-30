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

(-> {:row-vectors (->> [["2023-12-16"
                         nil
                         "Clay Calva integration - datavis demo"
                         "projects/visual-tools/clay-calva-demo-20231216/index.html"
                         "projects/visual-tools/clay-calva-demo-20231216"
                         "X_SsjhmG5Ok"
                         [:visual-tools :clay :calva :noj
                          :datavis :hanami :tablecloth]]
                        ["2023-12-17"
                         nil
                         "Clay CIDER integration - image processing demo"
                         "projects/visual-tools/clay-cider-demo-20231217/index.html"
                         "projects/visual-tools/clay-cider-demo-20231217"
                         "fd4kjlws6Ts"
                         [:visual-tools :clay :cider :noj
                          :image-processing :dtype-next :tensors]]
                        ["2023-12-31"
                         nil
                         "Reading HDF files"
                         "projects/data-formats/hdf/index.html"
                         "projects/data-formats/hdf/"
                         nil
                         [:data-formats :hdf :dtype-next :tensors]]
                        ["2024-01-11"
                         nil
                         "Machine learning - DRAFT"
                         "projects/noj/ml.html"
                         "projects/noj/notebooks/ml.clj"
                         nil
                         [:noj :ml :scicloj.ml :draft]]
                        ["2024-01-25"
                         nil
                         "Wolfram Lanauge interop with Wolframite"
                         "projects/math/wolframite/index.html"
                         "projects/math/wolframite/"
                         nil
                         [:math :wolframite :interop]]
                        ["2024-02-06"
                         "2024-03-19"
                         "Exploring ggplot"
                         "projects/datavis/ggplot/index.html"
                         "projects/datavis/ggplot/"
                         nil
                         [:noj :r :clojisr :interop :ggplot :datavis]]
                        ["2024-02-07"
                         nil
                         "Seattle parks & Neigborhoods - DRAFT"
                         "projects/geography/seattle-parks/index.html"
                         "projects/geography/seattle-parks"
                         nil
                         [:geography :gis :tablecloth :datavis :draft]]
                        ["2024-03-02"
                         nil
                         "Exploring Observable - DRAFT"
                         "projects/datavis/observable/index.html"
                         "projects/datavis/observable"
                         nil
                         [:datavis :observable :dashboards :draft]]
                        ["2024-03-24"
                         "2024-03-27"
                         "Chicago bike trips"
                         "projects/geography/chicago-bikes/index.html"
                         "projects/geography/chicago-bikes"
                         nil
                         [:geography :gis :tablecloth :datavis :noj :hanami :vega-lite]]
                        ["2024-03-30"
                         nil
                         "Noj getting started - from raw data to a blog post"
                         "https://scicloj.github.io/noj-getting-started"
                         "https://github.com/scicloj/noj-getting-started/tree/main"
                         nil
                         [:geography :gis :tablecloth :datavis :noj :hanami :vega-lite :clay :calva]]]
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
                                                   :href (str "https://github.com/scicloj/clojure-data-scrapbook/tree/main/"
                                                              source-path)}
                                               "(source)"]]
                                          (when draft [:p "(draft)"])]])
                                       (when youtube-id
                                         (kind/video {:youtube-id youtube-id}))
                                       (->> tags
                                            (map name)
                                            (str/join ", "))])))))
     :column-names ["created"
                    "updated"
                    "title"
                    "video"
                    "tags"]}
    (kind/table {:datatables {:paging false}})
    kindly/hide-code)

;; ## Contributing

;; (coming soon)
