(ns scicloj.scrapbook.tests
  (:require [tablecloth.api :as tc]
            [scicloj.kindly.v3.kind :as kind]
            [scicloj.kindly-default.v1.api :refer [is->]]))

;; ## Tests

;; ### clojure.test

;; Standard Clojure tests may be integrated into notebooks.

(require '[clojure.test :refer [deftest is]])

;; Tests returning a boolean value (as they usually do, ending with a check)
;; are rendered displaying that value as a clear x (failure) or v (success) mark:

(def test-dataset
  (tc/dataset {:x [1 2 3]
               :y [4 5 6]}))

(deftest mytest1
  (-> test-dataset
      tc/row-count
      (= 3)
      is))

;; Tests returning a non-boolean value are rendered simply displaying that value:

(deftest mytest2
  (-> test-dataset
      tc/row-count
      (= 3)
      is)
  test-dataset)

;; The `is->` function allows performing a few checks in a pipeline
;; and returning a different value to be displayed:

(deftest mytest3
  (-> 2
      (+ 3)
      (is-> > 4)
      (* 10)
      (is-> = 50)
      (* 10)))

;; These features open the way for literate testing / testable documentation solutions, such as those we have been using in the past (e.g., in [tutorials](https://scicloj.github.io/clojisr/doc/clojisr/v1/tutorial-test/) of ClojisR using Notespace v2).
