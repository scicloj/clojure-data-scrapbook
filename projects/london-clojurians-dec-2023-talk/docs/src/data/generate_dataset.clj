(ns data.generate-dataset
  (:require
   [clj-http.client :as client]
   [charred.api :as charred])
  )

(defn url [page-number]
  (str "https://api.github.com/search/repositories?q=language:clojure&order=desc&page="
       page-number
       "&per_page=50"))

(defn get-page! [url]
  (client/get url {:oauth-token (System/getenv "GITHUB_OAUTH_TOKEN")}))

(defn generate-ds-loop []
  (loop [result []
         page   1]
    (let [{:keys [items]} (-> page
                              url
                              get-page!
                              :body
                              (charred/read-json :key-fn keyword))]
      (if (seq items)
        (recur (into result items) (inc page))
        result))))

(defn generate-ds-iteration []
  (iteration get-page!
             :initk "https://api.github.com/search/repositories?q=language:clojure&order=desc&page=1&per_page=50"
             :vf (fn [response]
                   (let [items (-> response :body (charred/read-json :key-fn keyword))]
                     {:items items
                      :count (count items)}))
             :kf (fn [response] (-> response :links :next :href))))
