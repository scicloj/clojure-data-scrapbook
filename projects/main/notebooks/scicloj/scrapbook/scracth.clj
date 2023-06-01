
(ns scicloj.scrapbook.scratch
  (:require [clojure.repl]
            [tablecloth.api :as tc]
            [clojure.java.io :as io])
  (:import java.io.File))

(set! *warn-on-reflection* true)

(-> "."
    io/file
    (.getCanonicalPath))

(-> *ns*
    ())

((fn []
   (eval `(do
            (def x# 9)
            (meta #'x#)))))
