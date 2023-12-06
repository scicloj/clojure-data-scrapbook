(ns data.generate-dataset
  (:require
   [clj-http.client :as client]
   [charred.api :as charred]
   [tablecloth.api :as tc]
   [scicloj.tempfiles.api :as tempfiles]
   [clojure.edn :as edn]
   [again.core :as again]
   [clojure.pprint :as pp]
   [clojure.string :as string]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell])
  (:import java.time.format.DateTimeFormatter
           java.time.LocalDate))

(set! *warn-on-reflection* true)

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

(defn generate-ds-iteration [language]
  (sequence cat
       (iteration get-cached-page!
                  :initk (format "https://api.github.com/search/repositories?q=language:%s&order=desc&page=1&per_page=50"
                            (name language))
                  :vf (fn [response]
                        (-> response :body (charred/read-json :key-fn keyword) :items))
                  :kf (fn [response] (-> response :links :next :href)))))

(defn fresh-data [language]
  (->> language
       generate-ds-iteration
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
  (doseq [language [:clojure :python]]
    (->> language
         fresh-data
         pr-str
         (spit (timestamped-filename (str (name language) "-repos") "edn")))))

(def data
  (->> {:python "data/python-repos2023-12-05T14:46:13.281-00:00.edn"
        :clojure "data/clojure-repos2023-12-05T14:46:12.990-00:00.edn"}
       (map (fn [[language path]]
              [language (-> path
                            slurp
                            edn/read-string)]))
       (into {})))

(comment
  (->> data
       :clojure
       (mapcat (comp :items :items))
       (map (fn [repo]
              (-> repo
                  :contributors_url
                  #_slurp)))))

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

(def repos
  (->> data
       vals
       (mapcat (partial mapcat (comp :items :items)))
       (map #(-> %
                 (select-keys [:language :html_url :size :owner])
                 (update :owner :login)))))

(defn url->clone-path [url]
  (-> url
      (string/replace #"^https://github.com/" "")
      (string/replace #"/" "__")
      (->> (str "/workspace/clones-for-analysis/"))))

(comment
  (->> repos
       (filter (fn [{:keys [url size]}]
                 (< size 100000)))
       (run! (fn [{:keys [html_url]}]
               (let [clone-path (url->clone-path html_url)]
                 (prn [:handling html_url])
                 (io/make-parents clone-path)
                 (when-not (-> clone-path
                               io/file
                               (.exists))
                   (prn [:cloning-to clone-path])
                   (shell/sh "git"
                             "clone"
                             html_url
                             clone-path)))))))


(def commit-dates-collected
  (delay
    (->> repos
         (map (fn [{:as repo
                    :keys [language html_url]}]
                (prn [:git-log html_url])
                (-> html_url
                    url->clone-path
                    (->> (format "--git-dir=%s/.git"))
                    (#(shell/sh "git" %
                                "log"
                                "--pretty=format:\"%ad\""
                                "--date=short"))
                    :out
                    (string/replace #"\"" "")
                    string/split-lines
                    (->> (assoc repo :date))
                    tc/dataset)))
         (apply tc/concat))))

(comment
  (let [path (str "data/commit-dates-"
                  (timestamp)
                  ".csv.gz")]
    [path
     (-> @commit-dates-collected
         (tc/write! path))]))

(def commit-dates
  (-> "data/commit-dates-2023-12-06T15:09:51.065-00:00.csv.gz"
      (tc/dataset
       {:key-fn keyword})))





;; collect repos from the API
;; e.g. the 1000 most popular Clojure repos
;; e.g. the 1000 most popular Python repos

;; clone the repos
;; 1000 Clojure
;; 9?? Python

;; take the commit dates out of git-log
