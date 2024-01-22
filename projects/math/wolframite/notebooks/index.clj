;; imitating:
;; https://github.com/scicloj/wolframite/blob/main/dev/dev.clj

(ns index
  (:refer-clojure
   ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.core :as wl]))

(defonce loaded
  (do (println "Loading Wolfram symbols, this make take a few seconds...")
      (wl/load-all-symbols (symbol (ns-name *ns*)))
      :ok))

;;
;; Evaluation
;;

;; Use eval with a quoted Wolfram-as-clojure-data expression (`Fn[..]` becoming `(Fn ..)`):
(wl/eval '(Dot [1 2 3] [4 5 6])) ; Wolfram: `Dot[{1, 2, 3}, {4, 5, 6}]]`
(Dot [1 2 3] [4 5 6]) ; We have loaded all symbols as Clojure fns and thus can run this directly

(wl/eval '(N Pi 20))
(N 'Pi 20) ; Beware: Pi must still be quoted, it is not a fn

;;
;; Built-in documentation:
;;
(clojure.repl/doc Dot)
