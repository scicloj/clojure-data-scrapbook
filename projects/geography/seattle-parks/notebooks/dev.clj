(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

#_(clay/make! {:format [:quarto :html]
               :base-source-path "notebooks"
               :source-path ["index.clj"]
               :base-target-path "docs"
               :book {:author "Timothy Prately and Daniel Slutsky"
                      :title "Seattle Parks & Neighborhoods - DRAFT"}
               :clean-up-target-dir true})

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"]
             :base-target-path "docs"
             :clean-up-target-dir true})
