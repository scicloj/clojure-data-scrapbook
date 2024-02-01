(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           "ml.clj"
                           "ggplot.clj"]
             :base-target-path "docs"
             :book {:title "Noj tutorials"}
             :clean-up-target-dir true})
