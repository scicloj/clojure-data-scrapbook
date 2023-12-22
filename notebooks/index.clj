^:kindly/hide-code?
(ns index)



^{:kindly/hide-code? true
  :kindly/kind :kind/hiccup}
(->> [["Dec. 16th 2023"
       "Clay Calva integration - datavis demo"
       "projects/visual-tools/clay-calva-demo-20231216/scratch"
       "projects/visual-tools/clay-calva-demo-20231216"
       "X_SsjhmG5Ok"]
      ["Dec. 17th 2023"
       "Clay CIDER integration - image processing demo"
       "projects/visual-tools/clay-cider-demo-20231217/scratch"
       "projects/visual-tools/clay-cider-demo-20231217"
       "fd4kjlws6Ts"]]
     (map (fn [[date title url source-path youtube-id]]
            [:tr
             [:td date]
             [:td [:a {:href url}
                   title]]
             [:td [:a {:href source-path}
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
