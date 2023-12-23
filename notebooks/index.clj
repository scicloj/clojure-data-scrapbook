^{:kindly/hide-code true
  :clay {:quarto {:format {:html {:toc false
                                  :theme :spacelab}}
                  :highlight-style :solarized
                  :code-block-background true
                  :include-in-header {:text "<link rel = \"icon\" href = \"data:,\" />"}}}}
(ns index
  (:require [clojure.string :as str]
            [scicloj.kindly.v4.kind :as kind]))

;; # Clojure Data Scrapbook

^{:kindly/hide-code true}
(kind/hiccup
 [:img {:src "https://scicloj.github.io/sci-cloj-logo-transparent.png"
        :alt "SciCloj logo"
        :width "100"
        :align "right"}])

;; The Clojure Data Scrapbook is a collection of
;; community-contributed examples for the emerging Clojure data stack.
;;
;; You can interact with the examples by cloning this project and starting a REPL.
;;
;; This project is part of the [SciCloj community](https://scicloj.github.io/docs/community/about/).

;; ## Tutorials

^{:kindly/hide-code true}
(-> {:row-vectors (->> [["2023-12-16"
                         "Clay Calva integration - datavis demo"
                         "projects/visual-tools/clay-calva-demo-20231216/index.html"
                         "projects/visual-tools/clay-calva-demo-20231216"
                         "X_SsjhmG5Ok"
                         [:visual-tools :clay :calva :noj :datavis :hanami :tablecloth]]
                        ["2023-12-17"
                         "Clay CIDER integration - image processing demo"
                         "projects/visual-tools/clay-cider-demo-20231217/index.html"
                         "projects/visual-tools/clay-cider-demo-20231217"
                         "fd4kjlws6Ts"
                         [:visual-tools :clay :cider :noj :image-processing :dtype-next :tensors]]]
                       (map (fn [[date title url source-path youtube-id tags]]
                              [date
                               (kind/hiccup [:div
                                             [:pre
                                              [:p [:a {:href url}
                                                   title]]
                                              [:p [:a {:style {:background-color "#fdf6e3"}
                                                       :href (str "https://github.com/scicloj/clojure-data-scrapbook/tree/main/"
                                                                  source-path)}
                                                   "(source)"]]]])
                               (kind/hiccup [:iframe
                                             {:src (str "https://www.youtube.com/embed/" youtube-id)
                                              :allowfullscreen "allowfullscreen"}])
                               (->> tags
                                    (map name)
                                    (str/join ", "))])))
     :column-names ["date"
                    "title"
                    "video"
                    "tags"]}
    (kind/table {:datatables
                 {:paging false}}))

;; ## Contributing

;; (coming soon)
