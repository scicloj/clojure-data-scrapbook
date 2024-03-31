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

(-> {:row-vectors (->> "notebooks/toc.edn"
                       slurp
                       clojure.edn/read-string
                       (map (fn [{:keys [created updated
                                         title url source-path youtube-id tags]}]
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
