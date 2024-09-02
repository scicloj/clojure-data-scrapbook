(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :show false
             :source-path ["index.clj"
                           "using_clojisr.clj"
                           "representing.clj"
                           "comparing.clj"
                           "study_session_20240225.clj"
                           ;"ggplotly.clj" fails with Execution error (StackOverflowError) at clojisr.v1.codegen/seq-form->code (codegen.clj:210).
                           ;"ggplotly_cont.clj" fails with Execution error (StackOverflowError) at clojisr.v1.codegen/seq-form->code (codegen.clj:210).

                           ;"draft.clj" fails with Execution error (StackOverflowError) at clojisr.v1.codegen/seq-form->code (codegen.clj:210).
                           ]
             :base-target-path "docs"
             :book {:title "Exploring ggplot"}
             :clean-up-target-dir true})
