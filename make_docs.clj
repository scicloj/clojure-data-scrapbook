#!/usr/bin/env bb

;; This script is used by the Scrapbook's editors
;; to deploy the rendered documents.
;; It should be documented better.
;;
;; Please do not use it if you do not need to.
;; Note that it erases some directories.

(->> "projects"
     (shell/sh "find")
     :out
     str/split-lines
     (filter (partial re-matches #".*/docs$"))
     (mapcat (fn [source]
               (let [target (-> source
                                (str/replace #"/docs$" "")
                                (->> (str "docs/")))
                     target-parent (-> target
                                       (str/split #"/")
                                       butlast
                                       (->> (str/join "/")))]
                 [["rm" "-r" target]
                  ["mkdir" "-p" target-parent]
                  ["mv" source target]])))
     (run! (fn [cmd]
             (prn cmd)
             (prn (apply shell/sh cmd)))))
