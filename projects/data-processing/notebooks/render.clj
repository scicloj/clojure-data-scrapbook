(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           "many_json_lines.clj" ;fails for me with index-out-of-bounds exception
                           ]
             :base-target-path "docs"
             :book {:title "Data processing"}
             :clean-up-target-dir true
             :show false})
(System/exit 0)
