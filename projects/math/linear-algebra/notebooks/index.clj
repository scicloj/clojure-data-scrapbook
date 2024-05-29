(ns index
  (:require [uncomplicate.commons.core :refer [with-release]]
            [uncomplicate.fluokitten.core :refer [foldmap]]
            [uncomplicate.clojurecl.core :as opencl]
            [uncomplicate.neanderthal
             [core :refer [dot copy asum copy! row mv mm rk axpy entry!
                           subvector trans mm! zero]]
             [vect-math :refer [mul]]
             [native :refer [dv dge fge]]
             [opencl :as cl :refer [clv]]
             [random :refer [rand-uniform!]]]))


(mv
 (dge [[1 34 4]
       [31 4 5]
       [1 4 1]])
 (dv [3 1 4]))
