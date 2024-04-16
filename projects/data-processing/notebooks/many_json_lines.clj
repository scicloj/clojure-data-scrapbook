(load-file "../../header.edn")

;; # Processing many JSON lines

(ns many-json-lines
  (:require [charred.api :as charred]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.java.io :as io]))

;; Let us create a file with many JSON lines.

(def path "/tmp/lines.jsonl")

(with-open [writer ((charred/json-writer-fn {}) path)]
  (dotimes [i 3]
    (.writeObject writer
                  {:i i
                   :x (rand)})
    (.write (.w writer)
            "\n")))

;; Let us look at a few of those lines we wrote.

(with-open [reader (io/reader path)]
  (->> reader
       line-seq
       (take 4)
       vec))

;; Now we will demonstrate processing the data.
;; We will read all lines, extract the relevant part of each line,
;; and sum these parts up.

(defn extract-relevant-data [json-data]
  (get json-data "x"))


(with-open [json-fn (-> path
                        io/reader
                        (charred/read-json-supplier {:eof-error? false}))]
  (doall
   (map extract-relevant-data json-fn)))
