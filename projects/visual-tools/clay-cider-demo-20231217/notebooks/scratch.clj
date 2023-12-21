;; # Image processing

(ns scratch
  (:require [tech.v3.libs.buffered-image :as bufimg]
            [tech.v3.datatype :as dtype]
            [tech.v3.tensor :as tensor]
            [tech.v3.datatype.functional :as fun]
            [scicloj.noj.v1.vis.image :as vis.image]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Arithmetic

(+ 1 2)

;; ## Loading data

(defonce raw-image
  (bufimg/load
   "https://upload.wikimedia.org/wikipedia/commons/1/1e/Gay_head_cliffs_MV.JPG"))

(type raw-image)

(bufimg/image-type raw-image)

;; ## Displaying images

raw-image

;; ## Tensors

(defonce raw-tensor
  (-> raw-image
      bufimg/as-ubyte-tensor))

(dtype/shape raw-tensor)

;; ## Processing

[raw-image
 (-> raw-tensor
     (fun/* 0.5)
     (vis.image/tensor->image :byte-bgr))]

;; ## Hiccup

(kind/hiccup
 [:div
  [:h3 "raw image"]
  raw-image
  [:h3 "darkened image"]
  (-> raw-tensor
      (fun/* 0.5)
      (vis.image/tensor->image :byte-bgr))])

;; ## Colour channels

(def colour-channels
  (-> raw-tensor
      (tensor/slice-right 1)))

(def blue (colour-channels 0))
(def green (colour-channels 1))
(def red (colour-channels 2))

(count colour-channels)

(mapv dtype/shape colour-channels)

(-> (tensor/compute-tensor (dtype/shape raw-tensor)
                           (fn [i j k]
                             (if (= k 2)
                               (raw-tensor i j k)
                               0))
                           :uint8)
    (vis.image/tensor->image :byte-bgr))

;; ## Conditioned processing

(-> (tensor/compute-tensor (dtype/shape raw-tensor)
                           (fn [i j k]
                             (*
                              (raw-tensor i j k)
                              (if (> (green i j)
                                     (blue i j))
                                0.3
                                1)))
                           :uint8)
    (vis.image/tensor->image :byte-bgr))




(-> (tensor/compute-tensor (dtype/shape raw-tensor)
                           (fn [i j k]
                             (*
                              (raw-tensor i j k)
                              (if (> (green i j)
                                     (* 1.2 (blue i j)))
                                0.3
                                1)))
                           :uint8)
    (vis.image/tensor->image :byte-bgr))


(->> [0.7 0.8 0.9 1 1.1 1.2 1.3]
     (map (fn [factor]
            [factor
             (-> (tensor/compute-tensor (dtype/shape raw-tensor)
                                        (fn [i j k]
                                          (*
                                           (raw-tensor i j k)
                                           (if (> (green i j)
                                                  (* factor (blue i j)))
                                             0.3
                                             1)))
                                        :uint8)
                 (vis.image/tensor->image :byte-bgr))])))
