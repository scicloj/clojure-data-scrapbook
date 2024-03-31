^{:kindly/hide-code true
  :clay {:quarto {:title "Clojure Data Scrapbook"}}}

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

;; ## Discussion

;; You are encouraged to discuss your suggestoins, criticism, and questions with us. You may use the repo's [Issues](https://github.com/scicloj/clojure-data-scrapbook/issues) or the usual Scicloj ways of [contact](https://scicloj.github.io/docs/community/contact/).

;; ## Contributing

;; There are two ways to contribute to the scrapbook. Both are encouraged, depending on your convenience.

;; A. Using your own repo
;; - Create a repo with a [Clay](https://scicloj.github.io/clay/) notebook, multiple notebooks, or a book. Add a link to the scrapbook at the top of your notebooks. For an example repo, see [noj-getting-started](https://github.com/scicloj/noj-getting-started/tree/main).
;; - Create a [Pull Request](https://github.com/scicloj/clojure-data-scrapbook/pulls) adding it at the bottom of the [table of contents EDN file](https://github.com/scicloj/clojure-data-scrapbook/blob/main/notebooks/toc.edn).

;; B. Updating the scrapbook repo
;; - Please discuss it first with us, so that we think about the appropirate location in the repo.
;; - Add (or update) notebooks or books under the scrapbook's [repo](https://github.com/scicloj/clojure-data-scrapbook/) itself. Begin your commit messages with the name of the relevant notebook or book. E.g., "Random Forest Tutorial - added data visualizations".

;; ## Contents

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
