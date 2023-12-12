(ns notebooks.commit-details-gradually
  (:require [tablecloth.api :as tc]
            [scicloj.noj.v1.vis.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.datatype.rolling :as rolling]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.dataset.print :as print]
            [data.generate-dataset]
            [clojure.math :as math]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.noj.v1.stats :as stats]
            [scicloj.clay.v2.api :as clay]
            [clojure.string :as string]
            [clojure.java.shell :as shell]
            [scicloj.kind-portal.v1.api :as kind-portal]
            [portal.api :as portal]))


(def *commit-details
  (atom []))


(defn current-commit-details []
  (apply tc/concat @*commit-details))


(defn current-report []
  (-> (current-commit-details)
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
       :log-stargazers
       :log-contributors
       {:X :log-contributors
        :XSCALE {:zero false}
        :YSCALE {:zero false
                 :domain [5 12]}
        :line-options {:MCOLOR "brown"}})))

(defn collect-repo-commit-details! [{:as repo
                                     :keys [language html_url]}]
  (prn [:git-log html_url])
  (swap! *commit-details
         conj (-> html_url
                  data.generate-dataset/url->clone-path
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
                  tc/dataset)))

(defn collect-commit-details! []
  (reset! *commit-details [])
  (->> data.generate-dataset/repos
       (filter #(-> % :language (= "Clojure")))
       (partition 100)
       (run! (fn [batch]
               (->> batch
                    (run! collect-repo-commit-details!))
               (kind-portal/kindly-submit-context
                {:value (kind/hiccup
                         [:h1 (count @*commit-details) " repos handled"])})
               (kind-portal/kindly-submit-context
                {:value (current-report)})))))

(comment
  (collect-commit-details!))
