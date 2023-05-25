(ns scrapbook.data.hdf
  (:require [babashka.fs :as fs]
            [tech.v3.tensor :as tensor]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import io.jhdf.HdfFile
           java.io.File))

;; # Processing HDF files

(set! *warn-on-reflection* true)

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

(def tensors
  (hdf5->tensors "data/test.h5"))

(count tensors)

(take 3 tensors)
