(ns render_index)
(require '[scicloj.clay.v2.api :as clay])
(clay/make! {:format [:html] :show false :base-source-path "notebooks" :source-path ["index.clj"]})
(System/exit 0)
