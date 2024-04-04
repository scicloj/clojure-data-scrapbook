(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           "handbook/05_machine_learning/06_linear_regression.clj"]
             :base-target-path "docs"
             :book {:title "Python Data Science Handbook - in Clojure"}
             :clean-up-target-dir true})
