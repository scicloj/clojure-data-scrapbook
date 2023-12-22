^:kindly/hide-code?
(ns index)


;; # Clojure Data Scrapbook

^{:kindly/hide-code? true
  :kindly/kind :kind/hiccup}
[:img {:src "https://scicloj.github.io/sci-cloj-logo-transparent.png"
       :alt "SciCloj logo"
       :width "100"
       :align "right"}]

;; The Clojure Data Scrapbook is a collection of
;; community-contributed examples for the emerging Clojure data stack.
;;
;; You can interact with the examples by cloning this project and starting a REPL.
;;
;; This project is part of the [SciCloj community](https://scicloj.github.io/docs/community/about/).


^{:kindly/hide-code? true
  :kindly/kind :kind/hiccup}
(->> [["Dec. 16th 2023"
       "Clay Calva integration - datavis demo"
       "projects/visual-tools/clay-calva-demo-20231216/index.html"
       "projects/visual-tools/clay-calva-demo-20231216"
       "X_SsjhmG5Ok"]
      ["Dec. 17th 2023"
       "Clay CIDER integration - image processing demo"
       "projects/visual-tools/clay-cider-demo-20231217/index.html"
       "projects/visual-tools/clay-cider-demo-20231217"
       "fd4kjlws6Ts"]]
     (map (fn [[date title url source-path youtube-id]]
            [:tr
             [:td date]
             [:td [:a {:href url}
                   title]]
             [:td [:a {:href (str "https://github.com/scicloj/clojure-data-scrapbook/tree/main/"
                                  source-path)}
                   "source"]]
             [:td [:iframe
                   {:src (str "https://www.youtube.com/embed/" youtube-id)
                    :allowfullscreen "allowfullscreen"}]]]))
     (into [:table
            [:tr
             [:th "date"]
             [:th "title"]
             [:th "source"]
             [:th "video"]]]))
