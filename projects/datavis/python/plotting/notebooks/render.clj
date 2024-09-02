(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path "index.clj"
             :base-target-path "docs"
             :show false
             :clean-up-target-dir true})
(System/exit 0)
