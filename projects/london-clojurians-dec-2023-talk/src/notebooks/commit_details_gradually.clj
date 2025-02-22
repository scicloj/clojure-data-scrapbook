(ns notebooks.commit-details-gradually
  (:require [data.generate-dataset]
            [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.functional :as fun]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kind-portal.v1.api :as kind-portal]
            [portal.api :as portal]
            [clojure.string :as string]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]))

(defn url->clone-path [url]
  (-> url
      (string/replace #"^https://github.com/" "")
      (string/replace #"/" "__")
      (->> (str "/workspace/clones-for-gradual-analysis/"))))

(defn clone! [{:keys [html_url clone-path]}]
  (io/make-parents clone-path)
  (when-not (-> clone-path
                io/file
                (.exists))
    (prn [:git-clone html_url clone-path])
    (shell/sh "git"
              "clone"
              html_url
              clone-path)))


(defn clone-all! [])

(defn git-log [{:as repo
                :keys [html_url clone-path]}]
  (prn [:git-log clone-path])
  (-> clone-path
      (->> (format "--git-dir=%s/.git"))
      (#(shell/sh "git" %
                  "log"
                  "--pretty=format:\"%ad,%ae\""
                  "--date=short"))
      :out
      (string/replace #"\"" "")
      string/split-lines
      (->> (map (fn [line]
                  (-> line
                      (string/split #",")
                      ((fn [[date email]]
                         (merge repo
                                {:date date
                                 :email email})))))))
      tc/dataset
      (tc/set-dataset-name html_url)))


(defonce *stop (atom false))
(defonce *raw-commit-logs (atom []))
(defonce *portal-commit-logs (atom []))

(defn collect! [limit]
  (reset! *stop false)
  (reset! *raw-commit-logs [])
  (reset! *portal-commit-logs [])
  (-> data.generate-dataset/repos-ds
      (tc/map-columns :clone-path [:html_url]
                      url->clone-path)
      (tc/select-rows #(and (-> % :size (< 100000))
                            (-> % :language (= "Clojure"))))
      (tc/rows :as-maps)
      (->> (take limit)
           (mapv (fn [repo]
                   (prn repo)
                   (when-not @*stop
                     (clone! repo)
                     (let [log (git-log repo)]
                       (swap! *raw-commit-logs
                              conj log)
                       (swap! *portal-commit-logs
                              #(-> %
                                   (conj (kind-portal/prepare
                                          {:value log}))
                                   reverse))))))))
  (prn [:stopped]))

(comment
  (future (collect! 300))

  (reset! *stop true)

  (portal/open)
  (portal/submit *portal-commit-logs))


(defn report []
  (let [raw-commit-logs @*raw-commit-logs
        n (count raw-commit-logs)]
    [(kind/hiccup
      [:h1 n " repos"])
     (when (> n 10)
       (-> raw-commit-logs
           (->> (apply tc/concat))
           (tc/group-by [:language :html_url])
           (tc/aggregate {:n-contributors (fn [ds]
                                            (-> ds
                                                :email
                                                distinct
                                                count))})
           (tc/order-by [:n-contributors] :desc)
           (tc/left-join data.generate-dataset/repos-ds [:html_url])
           (tc/select-rows #(-> % :n-contributors (> 20)))
           (tc/add-columns {:log-stargazers #(-> %
                                                 :stargazers_count
                                                 fun/log)
                            :log-contributors #(-> %
                                                   :n-contributors
                                                   fun/log)})
           (stats/add-predictions :log-stargazers
                                  [:log-contributors]
                                  {:model-type :smile.regression/ordinary-least-square})
           (hanami/linear-regression-plot
            :log-contributors
            :log-stargazers
            {:XSCALE {:zero false
                      :domain [5 12]}
             :YSCALE {:zero false}
             :TOOLTIP [{:field "html_url"}]
             :point-options {:MSIZE 100}
             :line-options {:MCOLOR "brown"
                            :MSIZE 10
                            :OPACITY 0.5}})))]))

(def *report (atom nil))

(defn update-report! []
  (prn [:update-report])
  (reset! *report (kind-portal/prepare
                   {:value (report)})))

(comment

  (report)

  (update-report!)

  (add-watch *raw-commit-logs
             :report
             (fn [_ _ _ _]
               (update-report!)))

  (portal/submit *report))


(report)
