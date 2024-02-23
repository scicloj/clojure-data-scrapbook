(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           "using_clojisr.clj"
                           "representing.clj"]
             :base-target-path "docs"
             :book {:title "Exploring ggplot"}
             :clean-up-target-dir true})
