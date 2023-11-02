;; # Tests

(ns scicloj.scrapbook.tests
  (:require [tablecloth.api :as tc]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly-default.v1.api :refer [is-> md]]))

;; ## Using clojure.test in a notebook

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

;; ## Example

;; The following is an adaptation of a little part of the [Tablecloth documentation](https://scicloj.github.io/tablecloth/).
;;
;; We started adding calls to `dettest` and `is->` to see how these things feel and how much they do or do not interfere with the reading experience.
;;
;; See the [Zulip discussion](https://clojurians.zulipchat.com/#narrow/stream/151924-data-science/topic/testable.20docs.20.2F.20literate.20testing);

(def DS (tc/dataset {:V1 (take 9 (cycle [1 2]))
                     :V2 (range 1 10)
                     :V3 (take 9 (cycle [0.5 1.0 1.5]))
                     :V4 (take 9 (cycle ["A" "B" "C"]))}))

;; ### Group-by


;; Grouping by is an operation which splits dataset into subdatasets and pack it into new special type of... dataset. I distinguish two types of dataset: regular dataset and grouped dataset. The latter is the result of grouping.
;;
;; Grouped dataset is annotated in by `:grouped?` meta tag and consist following columns:
;;
;; * `:name` - group name or structure
;; * `:group-id` - integer assigned to the group
;; * `:data` - groups as datasets
;;
;; Almost all functions recognize type of the dataset (grouped or not) and operate accordingly.
;;
;; You can't apply reshaping or join/concat functions on grouped datasets.



;; #### Grouping

;; Grouping is done by calling `group-by` function with arguments:
;;
;; * `ds` - dataset
;; * `grouping-selector` - what to use for grouping
;; * options:
;; - `:result-type` - what to return:
;; * `:as-dataset` (default) - return grouped dataset
;; * `:as-indexes` - return rows ids (row number from original dataset)
;; * `:as-map` - return map with group names as keys and subdataset as values
;; * `:as-seq` - return sequens of subdatasets
;; - `:select-keys` - list of the columns passed to a grouping selector function
;;
;; All subdatasets (groups) have set name as the group name, additionally `group-id` is in meta.
;;
;; Grouping can be done by:
;;
;; * single column name
;; * seq of column names
;; * map of keys (group names) and row indexes
;; * value returned by function taking row as map (limited to `:select-keys`)
;;
;; Note: currently dataset inside dataset is printed recursively so it renders poorly from markdown. So I will use `:as-seq` result type to show just group names and groups.



;; List of columns in grouped dataset

(deftest columns-in-grouped-ds
  (-> DS
      (tc/group-by :V1)
      (is-> #(-> % tc/row-count (= 2)))
      (tc/column-names)
      (is-> = [:V1 :V2 :V3 :V4])))

;; List of columns in grouped dataset treated as regular dataset

(deftest columns-in-grouped-ds-as-regular-ds-test
  (-> DS
      (tc/group-by :V1)
      (tc/as-regular-dataset)
      (tc/column-names)
      (is-> = [:name :group-id :data])))

;; Content of the grouped dataset

(tc/columns (tc/group-by DS :V1) :as-map)

;; Grouped dataset as map

(keys (tc/group-by DS :V1 {:result-type :as-map}))

(vals (tc/group-by DS :V1 {:result-type :as-map}))

;; Group dataset as map of indexes (row ids)

(tc/group-by DS :V1 {:result-type :as-indexes})

;; Grouped datasets are printed as follows by default.

(tc/group-by DS :V1)

;; To get groups as sequence or a map can be done from grouped dataset using `groups->seq` and `groups->map` functions.
;;
;; Groups as seq can be obtained by just accessing `:data` column.
;;
;; I will use temporary dataset here.

(deftest groups-as-seq-1-test
  (let [ds (-> {"a" [1 1 2 2]
                "b" ["a" "b" "c" "d"]}
               (tc/dataset)
               (is-> #(-> % tc/row-count (= 4)))
               (tc/group-by "a")
               (is-> #(-> % tc/row-count (= 2))))]
    (-> (seq (ds :data)) ; seq is not necessary but Markdown treats `:data` as command here
        (is-> #(-> % count (= 2))))))

(deftest groups-as-seq-2-test
  (-> {"a" [1 1 2 2]
       "b" ["a" "b" "c" "d"]}
      (tc/dataset)
      (tc/group-by "a")
      (tc/groups->seq)
      (is-> #(-> % count (= 2)))))

;; Groups as map

(deftest groups-as-map-test
  (-> {"a" [1 1 2 2]
       "b" ["a" "b" "c" "d"]}
      (tc/dataset)
      (tc/group-by "a")
      (tc/groups->map)
      (is-> #(-> % count (= 2)))
      (is-> #(-> % keys (= [1 2])))))


;; Grouping by more than one column. You can see that group names are maps. When ungrouping is done these maps are used to restore column names.


(tc/group-by DS [:V1 :V3] {:result-type :as-seq})


;; Grouping can be done by providing just row indexes. This way you can assign the same row to more than one group.

(tc/group-by DS {"group-a" [1 2 1 2]
                 "group-b" [5 5 5 1]} {:result-type :as-seq})


;; You can group by a result of grouping function which gets row as map and should return group name. When map is used as a group name, ungrouping restore original column names.


(tc/group-by DS (fn [row] (* (:V1 row)
                             (:V3 row))) {:result-type :as-seq})


;; You can use any predicate on column to split dataset into two groups.



(tc/group-by DS (comp #(< % 1.0) :V3) {:result-type :as-seq})


;; `juxt` is also helpful

(tc/group-by DS (juxt :V1 :V3) {:result-type :as-seq})


;; `tech.ml.dataset` provides an option to limit columns which are passed to grouping functions. It's done for performance purposes.


(tc/group-by DS identity {:result-type :as-seq
                          :select-keys [:V1]})

;; #### Ungrouping


;; Ungrouping simply concats all the groups into the dataset. Following options are possible

;; * `:order?` - order groups according to the group name ascending order. Default: `false`
;; * `:add-group-as-column` - should group name become a column? If yes column is created with provided name (or `:$group-name` if argument is `true`). Default: `nil`.
;; * `:add-group-id-as-column` - should group id become a column? If yes column is created with provided name (or `:$group-id` if argument is `true`). Default: `nil`.
;; * `:dataset-name` - to name resulting dataset. Default: `nil` (_unnamed)

;; If group name is a map, it will be splitted into separate columns. Be sure that groups (subdatasets) doesn't contain the same columns already.

;; If group name is a vector, it will be splitted into separate columns. If you want to name them, set vector of target column names as `:add-group-as-column` argument.

;; After ungrouping, order of the rows is kept within the groups but groups are ordered according to the internal storage.
;; Grouping and ungrouping.



(-> DS
    (tc/group-by :V3)
    (tc/ungroup))


;; Groups sorted by group name and named.

(-> DS
    (tc/group-by :V3)
    (tc/ungroup {:order? true
                 :dataset-name "Ordered by V3"}))


;; Groups sorted descending by group name and named.

(-> DS
    (tc/group-by :V3)
    (tc/ungroup {:order? :desc
                 :dataset-name "Ordered by V3 descending"}))


;; Let's add group name and id as additional columns

(-> DS
    (tc/group-by (comp #(< % 4) :V2))
    (tc/ungroup {:add-group-as-column true
                 :add-group-id-as-column true}))


;; Let's assign different column names

(-> DS
    (tc/group-by (comp #(< % 4) :V2))
    (tc/ungroup {:add-group-as-column "Is V2 less than 4?"
                 :add-group-id-as-column "group id"}))


;; We can add group names without separation

(-> DS
    (tc/group-by (fn [row] {"V1 and V3 multiplied" (* (:V1 row)
                                                      (:V3 row))
                            "V4 as lowercase" (clojure.string/lower-case (:V4 row))}))
    (tc/ungroup {:add-group-as-column "just map"
                 :separate? false}))


;; The same applies to group names as sequences

(-> DS
    (tc/group-by (juxt :V1 :V3))
    (tc/ungroup {:add-group-as-column "abc"}))


;; Let's provide column names

(-> DS
    (tc/group-by (juxt :V1 :V3))
    (tc/ungroup {:add-group-as-column ["v1" "v3"]}))


;; Also we can supress separation

(-> DS
    (tc/group-by (juxt :V1 :V3))
    (tc/ungroup {:separate? false
                 :add-group-as-column true}))
;; => _unnamed [9 5]:




;; #### Other functions

;; To check if dataset is grouped or not just use `grouped?` function.



(tc/grouped? DS)





(tc/grouped? (tc/group-by DS :V1))

;; If you want to remove grouping annotation (to make all the functions work as with regular dataset) you can use `unmark-group` or `as-regular-dataset` (alias) functions.
;;
;; It can be important when you want to remove some groups (rows) from grouped dataset using `drop-rows` or something like that.

(-> DS
    (tc/group-by :V1)
    (tc/as-regular-dataset)
    (tc/grouped?))


;; You can also operate on grouped dataset as a regular one in case you want to access its columns using `without-grouping->` threading macro.

(-> DS
    (tc/group-by [:V4 :V1])
    (tc/without-grouping->
     (tc/order-by (comp (juxt :V4 :V1) :name))))


;; This is considered internal.
;;
;; If you want to implement your own mapping function on grouped dataset you can call `process-group-data` and pass function operating on datasets. Result should be a dataset to have ungrouping working.


(-> DS
    (tc/group-by :V1)
    (tc/process-group-data #(str "Shape: " (vector (tc/row-count %) (tc/column-count %))))
    (tc/as-regular-dataset))
