(load-file "../../../header.edn")

(ns index
  (:require [codegen :refer [obs]]))

;; # Observable examples

;; We will use the draft [codegen](./codegen) namespace.

;; ## A basic example

;; Note that we read the data from a file,
;; that we can conveniently generate in Clojure.

;; We follow [a basic example]((https://quarto.org/docs/interactive/ojs/libraries.html#plot))
;; from Quarto's Observable docs.

(obs
 '(def athletes
    (. (FileAttachment "notebooks/datasets/athletes.csv")
       (csv {:typed true}))))


(obs
 '(Plot.plot {:grid true
              :facet {:data athletes
                      :y "sex"}
              :marks [(Plot.rectY athletes
                                  (Plot.binX {:y "count"}
                                             {:x "weight" :fill "sex"}))
                      (Plot.ruleY [0])]}))

;; ## Interactions across components
;; Here, we reproduce
;; [the penguins example](https://quarto.org/docs/interactive/ojs/examples/penguins.html)
;; from the Quarto docs.

(obs
 '(viewof bill_length_min
          (Inputs.range [32 50]
                        {:value 35
                         :step 1
                         :label "Bill length (min):"}))
 '(viewof islands
          (Inputs.checkbox ["Torgersen" "Biscoe" "Dream"]
                           {:value ["Torgensen" "Biscoe"]
                            :lable "Islands:"}))
 '(. (Plot.rectY filtered
                 (Plot.binX {:y "count"}
                            {:x "body_mass_g"
                             :fill "species"
                             :thresholds 20}))
     (plot {:facet {:data filtered
                    :x "sex"
                    :y "species"
                    :marginRight 80}
            :marks [(Plot.frame)]}))
 '(Inputs.table filtered)
 '(def penguins (. (FileAttachment "notebooks/datasets/palmer-penguins.csv")
                   (csv {:typed true})))
 '(def filtered (. penguins
                   (filter [:js "function(penguin) {
                                           return bill_length_min < penguin.bill_length_mm &&
                                           islands.includes(penguin.island);
                                           }"]))))
