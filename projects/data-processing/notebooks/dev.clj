(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           "many_json_lines.clj"]
             :base-target-path "docs"
             :book {:title "Data processing"}
             :clean-up-target-dir true})
