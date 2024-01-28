(load-file "../../../header.edn")

;; This notebook demonstrates basic usage of [Wolframite](https://github.com/scicloj/wolframite/) in a way that would work in visual tools supporting [Kindly](https://scicloj.github.io/kindly-noted/kindly).

;; It is also appears at the Wolrfamite repo as [dev/kindly-demo.clj](https://github.com/scicloj/wolframite/blob/main/dev/kindly_demo.clj).

;; Note that to use Wolframite, you need Wolfamite in the dependencies,
;; as well as the `WOLFRAM_INSTALL_PATH`
;; set up in your system, as explained in
;; [Wolframite's README](https://github.com/scicloj/wolframite/).

(ns index
  (:refer-clojure
   ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.core :as wl]
   [wolframite.tools.hiccup :refer [view]]
   [wolframite.base.parse :as parse]
   [wolframite.jlink]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.kindly.v4.api :as kindly])
  (:import (com.wolfram.jlink MathCanvas KernelLink)
           (java.awt Color Frame)
           (java.awt.event WindowAdapter ActionEvent)))

^:kindly/hide-code
(def md
  (comp kindly/hide-code kind/md))

(md "## Make sure Wo")

(md "## Init (base example)")
^:note-to-test/skip

(wl/eval '(Dot [1 2 3] [4 5 6]))

(md "## Strings of WL code")

(wl/eval "{1 , 2, 3} . {4, 5, 6}")

(md "## Def // intern WL fns, i.e. effectively define WL fns as clojure fns:")

(def W:Plus (parse/parse-fn 'Plus {:kernel/link @wl/kernel-link-atom}))

(W:Plus 1 2 3) ; ... and call it


(def greetings
  (wl/eval
   '(Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings "Stephen")

(md " ## Bidirectional translation
(Somewhat experimental, especially in the wl->clj direction)")


(wl/->clj! "GridGraph[{5, 5}]")
(wl/->wl! '(GridGraph [5 5]) {:output-fn str})

(md "## Graphics")

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
