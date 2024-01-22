;; imitating:
;; https://github.com/scicloj/wolframite/blob/main/dev/*.clj

(ns index
  (:refer-clojure
   ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.core :as wl :refer [wl]]
   [wolframite.jlink])
  (:import (com.wolfram.jlink MathCanvas KernelLink)
           (java.awt Color Frame)
           (java.awt.event WindowAdapter ActionEvent)))


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



(defn make-math-canvas! [kernel-link]
  (doto (MathCanvas. kernel-link)
    (.setBounds 10, 25, 280, 240)
    (.setImageType MathCanvas/GRAPHICS)))

(defn make-app! [math-canvas]
  ;; (.evaluateToInputForm wl/kernel-link (str "Needs[\""  KernelLink/PACKAGE_CONTEXT "\"]") 0)
  ;; (.evaluateToInputForm wl/kernel-link "ConnectToFrontEnd[]" 0)
  (let [app (Frame.)]
    (doto app
      (.setLayout nil)
      (.setTitle "Wolframite Graphics")
      (.add math-canvas)
      (.setBackground Color/white)
      (.setSize 300 400)
      (.setLocation 50 50)
      (.setVisible true)
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowClosing [^ActionEvent e]
                              (.dispose app)))))))

(defn show!
  [math-canvas wl-form]
  (.setMathCommand math-canvas wl-form))

(comment

  (def canvas (make-math-canvas! wl/kernel-link))
  (def app (make-app! canvas))

  (show! canvas "GeoGraphics[]")

  (.dispose app))

;; TODO: improve
;; - better api (?)
;; - accept options
;; TODO: patch WL macro adding :show option
;; e.g.
;;
;; (WL :show (GeoGraphics))



(comment ;; fun is good

  (show! canvas "GeoGraphics[]")
  (show! canvas "Graph3D[GridGraph[{3, 3, 3}, VertexLabels -> Automatic]]")
  (show! canvas "GeoImage[Entity[\"City\", {\"NewYork\", \"NewYork\", \"UnitedStates\"}]]")

  )
