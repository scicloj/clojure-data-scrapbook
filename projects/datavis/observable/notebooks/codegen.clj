(ns codegen
  (:require [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]))

;; # Generating Observable code

;; This is a draft experiment of generating [Observable](https://observablehq.com/) code from Clojure forms.

(defn list-starting-with? [prefix form]
  (and (list? form)
       (-> form first (= prefix))))

(defn vector-starting-with? [prefix form]
  (and (vector? form)
       (-> form first (= prefix))))

(defn dot? [form]
  (list-starting-with? '. form))

(defn def? [form]
  (list-starting-with? 'def form))

(defn viewof? [form]
  (list-starting-with? 'viewof form))

(defn generated? [form]
  (vector-starting-with? :generated form))

(defn generated [string]
  (assert (string? string))
  [:generated string])

(defn generated->str [form]
  (second form))

(defn js? [form]
  (vector-starting-with? :js form))

(defn primitive? [form]
  (or (string? form)
      (number? form)
      (boolean? form)
      (symbol? form)))

(defn handle-form [form]
  (cond (generated? form) (generated->str form)
        (map? form) (->> form
                         (map (fn [[k v]]
                                (format "%s: %s"
                                        (name k) (handle-form v))))
                         (str/join ", ")
                         (format "{%s}"))
        (dot? form) (->> form
                         rest
                         (map handle-form)
                         (str/join "."))
        (def? form) (let [[lhs rhs] (rest form)]
                      (format "%s = %s"
                              (name lhs)
                              (handle-form rhs)))
        (viewof? form) (let [[lhs rhs] (rest form)]
                         (format "viewof %s = %s"
                                 (name lhs)
                                 (handle-form rhs)))
        (js? form) (-> form
                       second
                       str)
        (list? form)  (->> form
                           rest
                           (map handle-form)
                           (str/join ", ")
                           (format "%s(%s)" (-> form first name)))
        (vector? form) (->> form
                            (map handle-form)
                            (str/join ", ")
                            (format "[%s]"))
        (primitive? form) (pr-str form)))

(defn obs [& forms]
  (->> forms
       (map handle-form)
       (str/join "\n\n")
       kind/observable))
