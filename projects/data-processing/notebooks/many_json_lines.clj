(load-file "../../header.edn")

;; # Processing many JSON lines - DRAFT

;; This tutorial follows a discussion at the [real-world-data](https://scicloj.github.io/docs/community/groups/real-world-data/) group, where the following problem was proposed by Kevin:
;; * We have a huge file where each line is a JSON object.
;; * We wish to read, transform, and write each line without having to put the entire
;; file in memory.

;; There is an existing tutorial by [Alexander Yakushev]: [Building ETL Pipelines with Clojure and Transducers](https://www.grammarly.com/blog/engineering/building-etl-pipelines-with-clojure-and-transducers/) that demonstrates, in a similar context, clever use of transducers for memory-efficient and parallelized processing.

;; In the current tutorial, we try to demonstrate a more basic workflow.
;; We use [Charred](https://github.com/cnuernber/charred) for JSON processing.

(ns many-json-lines
  (:require [charred.api :as charred]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

;; Let us create a file with many JSON lines, a so-called JSONL file.

(def path "/tmp/lines.jsonl")

(def n-lines 1000000)

(with-open [writer ((charred/json-writer-fn {}) path)]
  (dotimes [i n-lines]
    (.writeObject writer
                  {:i i
                   :x (rand)})
    ;; Here we use an internal implementation detail of the JSONWriter
    ;; object created by Charred - it holds a regular Writer inside.
    ;; We will use it for the newline character.
    (.write (.w writer)
            "\n")))

;; We can check how big it is.

(:out (shell/sh "wc" path))

;; Let us look at a few of those lines we wrote.

(with-open [reader (io/reader path)]
  (->> reader
       line-seq
       (take 4)
       vec))

;; Now we will demonstrate processing the data.
;; We will read all lines, extract some part of each line,
;; and sum these numbers up.

(defn extract-relevant-data [json-data]
  (-> json-data
      (get "x")))

(defn json-supplier []
  (-> path
      io/reader
      (charred/read-json-supplier {:eof-error? false})))

;; This function returns a `JSONSUpplier` object,
;; which can be used as a lazy sequence.
;; Let us use if with a small number of lines.

(def small-n 100)

(with-open [json-lines (json-supplier)]
  (->> json-lines
       (map extract-relevant-data)
       (take small-n)
       (reduce +)))

;; This result makes sense: we sum up random numbers
;; which were sampled uniformely between 0 and 1,
;; so the expected sum is:

(* 0.5 small-n)

;; Lazy sequences keep all the data in memory.
;; Let us use our supplier in a more memory-efficient way.
