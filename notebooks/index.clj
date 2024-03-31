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
    (kind/table {:use-datatables true
                 :datatables {:scrollY 900}})
    kindly/hide-code)

;; ## Discussion

;; You are encouraged to discuss your suggestoins, criticism, and questions with us. You may use the repo's [Issues](https://github.com/scicloj/clojure-data-scrapbook/issues) or the usual Scicloj ways of [contact](https://scicloj.github.io/docs/community/contact/).

;; ## Contributing

;; See [notebooks/contributing.md](https://github.com/scicloj/clojure-data-scrapbook/blob/main/notebooks/contributing.md).

;; ## Known issues

;; See [notebooks/known_issues.md](https://github.com/scicloj/clojure-data-scrapbook/blob/main/notebooks/known_issues.md).
