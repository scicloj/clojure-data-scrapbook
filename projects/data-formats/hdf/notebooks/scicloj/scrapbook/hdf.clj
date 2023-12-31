^{:kindly/hide-code? true
  :kindly/kind :kind/hiccup}
[:table
 [:tr
  [:td "This is part of the Scicloj "
   [:a {:href "https://scicloj.github.io/clojure-data-scrapbook/"}
    "Clojure Data Scrapbook"]
   "."]
  [:a
   {:href "https://scicloj.github.io/clojure-data-scrapbook/"}
   [:img {:src "https://scicloj.github.io/sci-cloj-logo-transparent.png"
          :alt "SciCloj logo"
          :width "40"
          :align "left"}]]]]

;; # Reading HDF files

;; Original discussion at the Clojurians Zulip [chat](https://scicloj.github.io/docs/community/chat/): [#data-science > import hdf files](https://clojurians.zulipchat.com/#narrow/stream/151924-data-science/topic/import.20hdf.20files).

(ns scicloj.scrapbook.hdf
  (:require [babashka.fs :as fs]
            [tech.v3.tensor :as tensor]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [scicloj.noj.v1.vis.image :as vis.image]
            [tech.v3.datatype.functional :as fun])
  (:import io.jhdf.HdfFile
           java.io.File))

(set! *warn-on-reflection* true)

;; We will use the followig function
;; to read an [HDF](https://en.wikipedia.org/wiki/Hierarchical_Data_Format) file
;; using [jHDF](https://github.com/jamesmudd/jhdf/),
;; and convert it to [dtype-next](https://github.com/cnuernber/dtype-next) tensors.

(defn hdf5->tensors [path]
  (let [file ^File (io/file path)
        hdf-file ^HdfFile (HdfFile. file)
        children ^java.util.Map (.getChildren hdf-file)]
    (->> children
         keys
         (mapv (fn [key]
                 (let [child ^io.jhdf.dataset.ContiguousDatasetImpl (.get children key)
                       knew (keyword (first (string/split key
                                                          #" ")))]
                   {:key key
                    :data (-> child
                              .getData
                              tensor/->tensor)}))))))

;; Let us apply the function to a test file:

(def tensors
  (hdf5->tensors "data/test.h5"))

(count tensors)

(take 3 tensors)

;; Let us visualize a few of the tensors as images:

(->> tensors
     (take 3)
     (mapcat (fn [row]
               [row
                (-> row
                    :data
                    (fun/* 200)
                    (vis.image/tensor->image
                     :ushort-gray))])))
