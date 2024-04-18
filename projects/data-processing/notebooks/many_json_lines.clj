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
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

;; Let us create a file with many JSON lines, a so-called JSONL file.

(def path "/workspace/lines.jsonl")

(def n-lines 500000000)

#_(defonce run-once-1
    (with-open [writer ((charred/json-writer-fn {}) path)]
      (dotimes [i n-lines]
        (.writeObject writer
                      {:i i
                       :x (rand)
                       :y (rand)})
        ;; Here we use an internal implementation detail of the JSONWriter
        ;; object created by Charred - it holds a regular Writer inside.
        ;; We will use it for the newline character.
        (.write (.w writer)
                "\n"))))

;; We can check how big it is.

(-> (shell/sh "ls" "-lh" path)
    :out
    (str/split #" ")
    (nth 4))

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

(with-open [json-fn (json-supplier)]
  (->> json-fn
       (map extract-relevant-data)
       (take small-n)
       (reduce +)))

;; Alternatively:

(with-open [json-fn (json-supplier)]
  (transduce (comp (map extract-relevant-data)
                   (take small-n))
             +
             json-fn))

;; This result makes sense: we sum up random numbers
;; which were sampled uniformely between 0 and 1,
;; so the expected sum is:

(* 0.5 small-n)

;; Now let us go about processing the whole file.
;; Lazy sequences keep all the data in memory.
;; Let us use our supplier in a more memory-efficient way.

#_(time
   (with-open [json-fn (json-supplier)]
     (loop [sum 0
            line-data (.get json-fn)]
       (if-let [x (extract-relevant-data line-data)]
         (recur (+ sum x)
                (.get json-fn))
         (do (.close json-fn)
             sum)))))


(defn allocated-memory-MB []
  (let [r (Runtime/getRuntime)]
    (/ (- (.totalMemory r)
          (.freeMemory r))
       1000000.0)))


(with-open [json-fn (json-supplier)]
  (let [_ (System/gc)
        baseline-memory (allocated-memory-MB)
        s (->> json-fn
               seq
               (take 1000000))]
    [(reduce + (map extract-relevant-data s))
     (allocated-memory-MB)]))


(with-open [json-fn (json-supplier)]
  (let [_ (System/gc)
        baseline-memory (allocated-memory-MB)
        s (->> json-fn
               seq
               (take 1000000))]
    [(reduce + (map extract-relevant-data s))
     (count s)
     (allocated-memory-MB)]))

(let [_ (System/gc)
      baseline-memory (allocated-memory-MB)
      s (->> rand
             (repeatedly 10000000))]
  [(reduce + s)
   (count s)
   (- (allocated-memory-MB) baseline-memory)])


(defn allocated-memory-MB []
  (let [r (Runtime/getRuntime)]
    (/ (- (.totalMemory r)
          (.freeMemory r))
       1000000.0)))

(defn measure-sequence-consumption-seq [summary-fn]
  (let [_ (System/gc)
        baseline-memory (allocated-memory-MB)
        s (take 100000000 (repeat 1))]
    {:summary (summary-fn s)
     :memory-usage-MB (- (allocated-memory-MB) baseline-memory)}))

[(repeatedly
  5
  #(measure-sequence-consumption-seq
    (fn [s]
      (reduce + s))))
 (repeatedly
  5
  #(measure-sequence-consumption-seq
    (fn [s]
      [(reduce + s)
       (count s)])))
 (repeatedly
  5
  #(measure-sequence-consumption-seq
    (juxt count count)))]




(defn measure-sequence-consumption-jsonl [summary-fn]
  (let [_ (System/gc)
        baseline-memory (allocated-memory-MB)
        s (->> (json-supplier)
               (take 1000000)
               (map extract-relevant-data))]
    {:summary (summary-fn s)
     :memory-usage-MB (- (allocated-memory-MB) baseline-memory)}))


[(repeatedly
  5
  #(measure-sequence-consumption-jsonl
    (fn [s]
      [(reduce + s)
       (count s)])))
 (repeatedly
  5
  #(measure-sequence-consumption-jsonl
    (fn [s]
      (reduce + s))))]
