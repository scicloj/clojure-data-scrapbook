(load-file "../../../header.edn")

(ns index
  (:refer-clojure
   ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.core :as wl :refer [wl]]
   [wolframite.tools.hiccup]
   [wolframite.base.parse :as parse]
   [wolframite.jlink]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.kindly.v4.api :as kindly])
  (:import (com.wolfram.jlink MathCanvas KernelLink)
           (java.awt Color Frame)
           (java.awt.event WindowAdapter ActionEvent)))

(def md
  (comp kindly/hide-code kind/md))

(md "# Wolframite

This notebook demonstrates basic usage of [Wolframite](https://github.com/scicloj/wolframite/),

It is mostly copied and adpated from [the official demo](https://github.com/scicloj/wolframite/blob/main/dev/demo.clj).

## Init (base example)
")

(wl/eval '(Dot [1 2 3] [4 5 6]))

(md "## Strings of WL code")

(wl/eval "{1 , 2, 3} . {4, 5, 6}")

(md "## Def // intern WL fns, i.e. effectively define WL fns as clojure fns:")

(def W:Plus (parse/parse-fn 'Plus {:kernel/link @wl/kernel-link-atom}))

(W:Plus 1 2 3) ; ... and call it

(wl/clj-intern 'Plus {}) ; a simpler way to do the same -> fn Plus in this ns

(map wl/clj-intern ['Dot 'Plus])

(md " Call interned Wl functions:")
(Dot [1 2 3] [4 5 6])
(Plus 1 2 3)
(Plus [1 2] [3 4])

(def greetings
  (wl/eval
   '(Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings "Stephen")

(md " ## Bidirectional translation
(Somewhat experimental, especially in the wl->clj direction)")


(wl/->clj! "GridGraph[{5, 5}]")
(wl/->wl! '(GridGraph [5 5]) {:output-fn str})

(md "## Graphics

### A helper function to view things in tools supporting [Kindly](https://scicloj.github.io/kindly-noted/kindly)")

(defn view
  ([form]
   (view form nil))
  ([form {:keys [folded?]}]
   (-> form
       (wolframite.tools.hiccup/view* folded?)
       kind/hiccup)))

(md " ### Draw Something!")

(view
 '(GridGraph [5 5]))

(view
 '(GridGraph [5 5])
 {:folded? true})

(view
 '(ChemicalData "Ethanol" "StructureDiagram"))

(md "## More Working Examples")

(wl/eval '(GeoNearest (Entity "Ocean") Here))

(md " TODO: Make this work with `view` as well.")

(view '(TextStructure "The cat sat on the mat."))

(md "## Wolfram Alpha")

(wl/eval '(WolframAlpha "number of moons of Saturn" "Result"))

(view '(WolframAlpha "number of moons of Saturn" "Result"))
