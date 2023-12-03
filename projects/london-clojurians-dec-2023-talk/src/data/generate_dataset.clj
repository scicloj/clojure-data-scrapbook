(ns data.generate-dataset
  (:require
   [clj-http.client :as client]
   [charred.api :as charred]
   [tablecloth.api :as tc]
   [scicloj.tempfiles.api :as tempfiles]
   [clojure.edn :as edn]
   [again.core :as again]
   [clojure.pprint :as pp]
   [clojure.string :as string]))

(comment
  (tempfiles/cleanup-all-tempdirs!))

(comment
  (-> 1
      url
      get-page!
      :body
      (charred/read-json :key-fn keyword)
      :total_count))


(defn url [page-number]
  (str "https://api.github.com/search/repositories?q=language:clojure&order=desc&page="
       page-number
       "&per_page=50"))

(defn get-page! [url]
  (prn [:get-page! url])
  (again/with-retries
    (->> 1
         (iterate (partial * 4))
         (take 9)
         vec)
    (client/get url {:oauth-token (System/getenv "GITHUB_OAUTH_TOKEN")})))

(def cached-resource
  (memoize
   (fn [url]
     (let [path (-> ".json"
                    tempfiles/tempfile!
                    :path)]
       (-> url
           get-page!
           (dissoc :http-client :headers)
           pr-str
           (->> (spit path)))
       path))))

(defn get-cached-page! [url]
  (-> url
      cached-resource
      slurp
      edn/read-string))

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
  (iteration get-cached-page!
             :initk "https://api.github.com/search/repositories?q=language:clojure&order=desc&page=1&per_page=50"
             :vf (fn [response]
                   (let [items (-> response :body (charred/read-json :key-fn keyword))]
                     {:items items
                      :count (count items)}))
             :kf (fn [response] (-> response :links :next :href))))

(defn fresh-data []
  (->> (generate-ds-iteration)
       vec
       time))

(defn timestamp []
  (-> (java.util.Date.)
      pr-str
      (subs 6)
      read-string))

(defn timestamped-filename [name ext]
  (str "data/"
       name
       (timestamp)
       "."
       ext))

(comment
  (->> (fresh-data)
       pr-str
       (spit (timestamped-filename "repos" "edn"))))

(defonce data
  (-> "data/repos2023-11-30T16:13:12.877-00:00.edn"
      slurp
      edn/read-string))


(comment
  (->> data
       (mapcat (comp :items :items))
       (map (fn [repo]
              (-> repo
                  :contributors_url
                  slurp)))))

(defonce borkdude-repos
  (-> "https://api.github.com/users/borkdude/repos"
      (client/get {:oauth-token (System/getenv "GITHUB_OAUTH_TOKEN")})
      :body
      (charred/read-json :key-fn keyword)
      (->> (map :html_url))))

(defonce babashka-repos
  (-> "https://api.github.com/users/babashka/repos"
      (client/get {:oauth-token (System/getenv "GITHUB_OAUTH_TOKEN")})
      :body
      (charred/read-json :key-fn keyword)
      (->> (map :html_url))))
